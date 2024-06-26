/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.configuration;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import javax.persistence.Column;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.*;
import org.hibernate.envers.configuration.metadata.AuditTableData;
import org.hibernate.envers.configuration.metadata.MetadataTools;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.revisioninfo.*;
import org.hibernate.envers.tools.MutableBoolean;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class RevisionInfoConfiguration {
    private String revisionInfoEntityName;
    private PropertyData revisionInfoIdData;
    private PropertyData revisionInfoTimestampData;
    private PropertyData modifiedEntityNamesData;
    private Type revisionInfoTimestampType;
    private GlobalConfiguration globalCfg;

    private String revisionPropType;
    private String revisionPropSqlType;

    public RevisionInfoConfiguration(GlobalConfiguration globalCfg) {
        this.globalCfg = globalCfg;
        revisionInfoEntityName = "org.hibernate.envers.DefaultRevisionEntity";
        revisionInfoIdData = new PropertyData("id", "id", "field", null);
        revisionInfoTimestampData = new PropertyData("timestamp", "timestamp", "field", null);
        modifiedEntityNamesData = new PropertyData("modifiedEntityNames", "modifiedEntityNames", "field", null);
        revisionInfoTimestampType = new LongType();

        revisionPropType = "integer";
    }

    private Document generateDefaultRevisionInfoXmlMapping() {
        Document document = DocumentHelper.createDocument();

        Element class_mapping = MetadataTools.createEntity(document, new AuditTableData(null, null, globalCfg.getDefaultSchemaName(), globalCfg.getDefaultCatalogName()), null);

        class_mapping.addAttribute("name", revisionInfoEntityName);
        class_mapping.addAttribute("table", "REVINFO");

        Element idProperty = MetadataTools.addNativelyGeneratedId(class_mapping, revisionInfoIdData.getName(),
                revisionPropType);
        MetadataTools.addColumn(idProperty, "REV", null, 0, 0, null, null, null, false);

        Element timestampProperty = MetadataTools.addProperty(class_mapping, revisionInfoTimestampData.getName(),
                revisionInfoTimestampType.getName(), true, false);
        MetadataTools.addColumn(timestampProperty, "REVTSTMP", null, 0, 0, null, null, null, false);

        if (globalCfg.isTrackEntitiesChangedInRevisionEnabled()) {
            generateEntityNamesTrackingTableMapping(class_mapping, "modifiedEntityNames", "REVENTITY", "REV", "ENTITYNAME", "string");
        }

        return document;
    }

    /**
     * Generates mapping that represents a set of strings.<br />
     * <code>
     * &lt;set name="propertyName" table="joinTableName" cascade="persist, delete" lazy="false" fetch="join"&gt;<br />
     * &nbsp;&nbsp;&nbsp;&lt;key column="joinTablePrimaryKeyColumnName" /&gt;<br />
     * &nbsp;&nbsp;&nbsp;&lt;element type="joinTableValueColumnType"&gt;<br />
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;column name="joinTableValueColumnName" /&gt;<br />
     * &nbsp;&nbsp;&nbsp;&lt;/element&gt;<br />
     * &lt;/set&gt;
     * </code>
     */
    private void generateEntityNamesTrackingTableMapping(Element class_mapping, String propertyName,
                                                         String joinTableName, String joinTablePrimaryKeyColumnName,
                                                         String joinTableValueColumnName, String joinTableValueColumnType) {
        Element set = class_mapping.addElement("set");
        set.addAttribute("name", propertyName);
        set.addAttribute("table", joinTableName);
        set.addAttribute("cascade", "persist, delete");
        set.addAttribute("fetch", "join");
        set.addAttribute("lazy", "false");
        Element key = set.addElement("key");
        key.addAttribute("column", joinTablePrimaryKeyColumnName);
        Element element = set.addElement("element");
        element.addAttribute("type", joinTableValueColumnType);
        Element column = element.addElement("column");
        column.addAttribute("name", joinTableValueColumnName);
    }

    private Element generateRevisionInfoRelationMapping() {
        Document document = DocumentHelper.createDocument();
        Element rev_rel_mapping = document.addElement("key-many-to-one");
        rev_rel_mapping.addAttribute("type", revisionPropType);
        rev_rel_mapping.addAttribute("class", revisionInfoEntityName);

        if (revisionPropSqlType != null) {
            // Putting a fake name to make Hibernate happy. It will be replaced later anyway.
            MetadataTools.addColumn(rev_rel_mapping, "*" , null, 0, 0, revisionPropSqlType, null, null, false);
        }

        return rev_rel_mapping;
    }

    private void searchForRevisionInfoCfgInProperties(XClass clazz, ReflectionManager reflectionManager,
                                    MutableBoolean revisionNumberFound, MutableBoolean revisionTimestampFound,
                                    MutableBoolean modifiedEntityNamesFound, String accessType) {
        for (XProperty property : clazz.getDeclaredProperties(accessType)) {
            RevisionNumber revisionNumber = property.getAnnotation(RevisionNumber.class);
            RevisionTimestamp revisionTimestamp = property.getAnnotation(RevisionTimestamp.class);
            ModifiedEntityNames modifiedEntityNames = property.getAnnotation(ModifiedEntityNames.class);

            if (revisionNumber != null) {
                if (revisionNumberFound.isSet()) {
                    throw new MappingException("Only one property may be annotated with @RevisionNumber!");
                }

                XClass revisionNumberClass = property.getType();
                if (reflectionManager.equals(revisionNumberClass, Integer.class) ||
                        reflectionManager.equals(revisionNumberClass, Integer.TYPE)) {
                    revisionInfoIdData = new PropertyData(property.getName(), property.getName(), accessType, null);
                    revisionNumberFound.set();
                } else if (reflectionManager.equals(revisionNumberClass, Long.class) ||
                        reflectionManager.equals(revisionNumberClass, Long.TYPE)) {
                    revisionInfoIdData = new PropertyData(property.getName(), property.getName(), accessType, null);
                    revisionNumberFound.set();

                    // The default is integer
                    revisionPropType = "long";
                } else {
                    throw new MappingException("The field annotated with @RevisionNumber must be of type " +
                            "int, Integer, long or Long");
                }

                // Getting the @Column definition of the revision number property, to later use that info to
                // generate the same mapping for the relation from an audit table's revision number to the
                // revision entity revision number.
                Column revisionPropColumn = property.getAnnotation(Column.class);
                if (revisionPropColumn != null) {
                    revisionPropSqlType = revisionPropColumn.columnDefinition();
                }
            }

            if (revisionTimestamp != null) {
                if (revisionTimestampFound.isSet()) {
                    throw new MappingException("Only one property may be annotated with @RevisionTimestamp!");
                }

                XClass revisionTimestampClass = property.getType();
                if (reflectionManager.equals(revisionTimestampClass, Long.class) ||
                        reflectionManager.equals(revisionTimestampClass, Long.TYPE) ||
                        reflectionManager.equals(revisionTimestampClass, Date.class) ||
                        reflectionManager.equals(revisionTimestampClass, java.sql.Date.class)) {
                    revisionInfoTimestampData = new PropertyData(property.getName(), property.getName(), accessType, null);
                    revisionTimestampFound.set();
                } else {
                    throw new MappingException("The field annotated with @RevisionTimestamp must be of type " +
                            "long, Long, java.util.Date or java.sql.Date");
                }
            }

            if (modifiedEntityNames != null) {
                if (modifiedEntityNamesFound.isSet()) {
                    throw new MappingException("Only one property may be annotated with @ModifiedEntityNames!");
                }
                XClass modifiedEntityNamesClass = property.getType();
                if (reflectionManager.equals(modifiedEntityNamesClass, Set.class) &&
                        reflectionManager.equals(property.getElementClass(), String.class)) {
                    modifiedEntityNamesData = new PropertyData(property.getName(), property.getName(), accessType, null);
                    modifiedEntityNamesFound.set();
                } else {
                    throw new MappingException("The field annotated with @ModifiedEntityNames must be of type Set<String>.");
                }
            }
        }
    }

    private void searchForRevisionInfoCfg(XClass clazz, ReflectionManager reflectionManager,
                                          MutableBoolean revisionNumberFound, MutableBoolean revisionTimestampFound,
                                          MutableBoolean modifiedEntityNamesFound) {
        XClass superclazz = clazz.getSuperclass();
        if (!"java.lang.Object".equals(superclazz.getName())) {
            searchForRevisionInfoCfg(superclazz, reflectionManager, revisionNumberFound, revisionTimestampFound, modifiedEntityNamesFound);
        }

        searchForRevisionInfoCfgInProperties(clazz, reflectionManager, revisionNumberFound, revisionTimestampFound,
                modifiedEntityNamesFound, "field");
        searchForRevisionInfoCfgInProperties(clazz, reflectionManager, revisionNumberFound, revisionTimestampFound,
                modifiedEntityNamesFound, "property");
    }

    public RevisionInfoConfigurationResult configure(Configuration cfg, ReflectionManager reflectionManager) {
        Iterator<PersistentClass> classes = (Iterator<PersistentClass>) cfg.getClassMappings();
        boolean revisionEntityFound = false;
        RevisionInfoGenerator revisionInfoGenerator = null;

        Class<?> revisionInfoClass = null;

        while (classes.hasNext()) {
            PersistentClass pc = classes.next();
            XClass clazz;
            try {
                clazz = reflectionManager.classForName(pc.getClassName(), this.getClass());
            } catch (ClassNotFoundException e) {
                throw new MappingException(e);
            }

            RevisionEntity revisionEntity = clazz.getAnnotation(RevisionEntity.class);
            if (revisionEntity != null) {
                if (revisionEntityFound) {
                    throw new MappingException("Only one entity may be annotated with @RevisionEntity!");
                }

                // Checking if custom revision entity isn't audited
                if (clazz.getAnnotation(Audited.class) != null) {
                    throw new MappingException("An entity annotated with @RevisionEntity cannot be audited!");
                }

                revisionEntityFound = true;

                MutableBoolean revisionNumberFound = new MutableBoolean();
                MutableBoolean revisionTimestampFound = new MutableBoolean();
                MutableBoolean modifiedEntityNamesFound = new MutableBoolean();

                searchForRevisionInfoCfg(clazz, reflectionManager, revisionNumberFound, revisionTimestampFound, modifiedEntityNamesFound);

                if (!revisionNumberFound.isSet()) {
                    throw new MappingException("An entity annotated with @RevisionEntity must have a field annotated " +
                            "with @RevisionNumber!");
                }

                if (!revisionTimestampFound.isSet()) {
                    throw new MappingException("An entity annotated with @RevisionEntity must have a field annotated " +
                            "with @RevisionTimestamp!");
                }

                revisionInfoEntityName = pc.getEntityName();

                revisionInfoClass = pc.getMappedClass();
                revisionInfoTimestampType = pc.getProperty(revisionInfoTimestampData.getName()).getType();
                if (globalCfg.isTrackEntitiesChangedInRevisionEnabled() ||
                        DefaultTrackingModifiedTypesRevisionEntity.class.isAssignableFrom(revisionInfoClass) ||
                        modifiedEntityNamesFound.isSet()) {
                    // If tracking modified entities parameter is enabled, custom revision info entity is a subtype
                    // of DefaultTrackingModifiedTypesRevisionEntity class, or @ModifiedEntityNames annotation is used.
                    revisionInfoGenerator = new DefaultTrackingModifiedTypesRevisionInfoGenerator(revisionInfoEntityName,
                            revisionInfoClass, revisionEntity.value(), revisionInfoTimestampData, isTimestampAsDate(),
                            modifiedEntityNamesData);
                    globalCfg.setTrackEntitiesChangedInRevisionEnabled(true);
                } else {
                    revisionInfoGenerator = new DefaultRevisionInfoGenerator(revisionInfoEntityName, revisionInfoClass,
                            revisionEntity.value(), revisionInfoTimestampData, isTimestampAsDate());
                }
            }
        }

        // In case of a custom revision info generator, the mapping will be null.
        Document revisionInfoXmlMapping = null;

        if (revisionInfoGenerator == null) {
            if (globalCfg.isTrackEntitiesChangedInRevisionEnabled()) {
                revisionInfoClass = DefaultTrackingModifiedTypesRevisionEntity.class;
                revisionInfoEntityName = DefaultTrackingModifiedTypesRevisionEntity.class.getName();
                revisionInfoGenerator = new DefaultTrackingModifiedTypesRevisionInfoGenerator(revisionInfoEntityName, revisionInfoClass,
                        RevisionListener.class, revisionInfoTimestampData, isTimestampAsDate(), modifiedEntityNamesData);
            } else {
                revisionInfoClass = DefaultRevisionEntity.class;
                revisionInfoGenerator = new DefaultRevisionInfoGenerator(revisionInfoEntityName, revisionInfoClass,
                        RevisionListener.class, revisionInfoTimestampData, isTimestampAsDate());
            }
            revisionInfoXmlMapping = generateDefaultRevisionInfoXmlMapping();
        }

        return new RevisionInfoConfigurationResult(
                revisionInfoGenerator, revisionInfoXmlMapping,
                new RevisionInfoQueryCreator(revisionInfoEntityName, revisionInfoIdData.getName(),
                        revisionInfoTimestampData.getName(), isTimestampAsDate(),
                        globalCfg.isTrackEntitiesChangedInRevisionEnabled() ? modifiedEntityNamesData.getName()
                                                                            : null),
                generateRevisionInfoRelationMapping(),
                new RevisionInfoNumberReader(revisionInfoClass, revisionInfoIdData), revisionInfoEntityName,
                revisionInfoClass, revisionInfoTimestampData);
    }

    private boolean isTimestampAsDate() {
    	String typename = revisionInfoTimestampType.getName();
    	return "date".equals(typename) || "time".equals(typename) || "timestamp".equals(typename);
    }
}

class RevisionInfoConfigurationResult {
    private final RevisionInfoGenerator revisionInfoGenerator;
    private final Document revisionInfoXmlMapping;
    private final RevisionInfoQueryCreator revisionInfoQueryCreator;
    private final Element revisionInfoRelationMapping;
    private final RevisionInfoNumberReader revisionInfoNumberReader;
    private final String revisionInfoEntityName;
    private final Class<?> revisionInfoClass;
    private final PropertyData revisionInfoTimestampData;

    RevisionInfoConfigurationResult(RevisionInfoGenerator revisionInfoGenerator,
                                    Document revisionInfoXmlMapping, RevisionInfoQueryCreator revisionInfoQueryCreator,
                                    Element revisionInfoRelationMapping,
                                    RevisionInfoNumberReader revisionInfoNumberReader, String revisionInfoEntityName,  Class<?> revisionInfoClass,
                                    PropertyData revisionInfoTimestampData) {
        this.revisionInfoGenerator = revisionInfoGenerator;
        this.revisionInfoXmlMapping = revisionInfoXmlMapping;
        this.revisionInfoQueryCreator = revisionInfoQueryCreator;
        this.revisionInfoRelationMapping = revisionInfoRelationMapping;
        this.revisionInfoNumberReader = revisionInfoNumberReader;
        this.revisionInfoEntityName = revisionInfoEntityName;
        this.revisionInfoClass = revisionInfoClass;
        this.revisionInfoTimestampData = revisionInfoTimestampData;
    }

    public RevisionInfoGenerator getRevisionInfoGenerator() {
        return revisionInfoGenerator;
    }

    public Document getRevisionInfoXmlMapping() {
        return revisionInfoXmlMapping;
    }

    public RevisionInfoQueryCreator getRevisionInfoQueryCreator() {
        return revisionInfoQueryCreator;
    }

    public Element getRevisionInfoRelationMapping() {
        return revisionInfoRelationMapping;
    }

    public RevisionInfoNumberReader getRevisionInfoNumberReader() {
        return revisionInfoNumberReader;
    }

    public String getRevisionInfoEntityName() {
        return revisionInfoEntityName;
    }

	public Class<?> getRevisionInfoClass() {
		return revisionInfoClass;
	}

	public PropertyData getRevisionInfoTimestampData() {
		return revisionInfoTimestampData;
	}
}
