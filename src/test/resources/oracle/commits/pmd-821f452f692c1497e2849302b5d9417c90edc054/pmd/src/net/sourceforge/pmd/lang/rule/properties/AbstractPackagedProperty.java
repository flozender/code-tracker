package net.sourceforge.pmd.lang.rule.properties;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sourceforge.pmd.lang.rule.properties.factories.BasicPropertyDescriptorFactory;
import static net.sourceforge.pmd.PropertyDescriptorFields.*;

/**
 * Concrete subclasses manage items that reside within namespaces per the design of the Java language.
 * Rule developers can limit the range of permissible items by specifying portions of their package
 * names in the constructor. If the legalPackageNames value is set to null then no restrictions are
 * made.
 * 
 * @author Brian Remedios
 */
public abstract class AbstractPackagedProperty<T> extends AbstractProperty<T> {

	private String[] legalPackageNames;

	private static final char PACKAGE_NAME_DELIMITER = ' ';
	
	protected static final Map<String, Boolean> packagedFieldTypesByKey = BasicPropertyDescriptorFactory.expectedFieldTypesWith(
			new String[]  { LEGAL_PACKAGES}, 
			new Boolean[] { Boolean.FALSE}
			);
	
    
    protected static String[] packageNamesIn(Map<String, String> params) {
        // TODO
        return null;
    }
	
	/**
	 * 
	 * @param theName
	 * @param theDescription
	 * @param theDefault
	 * @param theLegalPackageNames
	 * @param theUIOrder
	 * @throws IllegalArgumentException
	 */
	protected AbstractPackagedProperty(String theName, String theDescription, T theDefault, String[] theLegalPackageNames, float theUIOrder) {
		super(theName, theDescription, theDefault, theUIOrder);
		
		checkValidPackages(theDefault, theLegalPackageNames);
		
		legalPackageNames = theLegalPackageNames;
	}
	
    /**
     * @param attributes Map<String,String>
     */
    protected void addAttributesTo(Map<String, String> attributes) {
        super.addAttributesTo(attributes);
        
        attributes.put(LEGAL_PACKAGES, delimitedPackageNames());
    }
	
    /**
     * @return String
     */
    private final String delimitedPackageNames() {
        
        if (legalPackageNames == null || legalPackageNames.length == 0) { return ""; }
        if (legalPackageNames.length == 1) { return legalPackageNames[0];  }
        
        StringBuilder sb = new StringBuilder();
        sb.append(legalPackageNames[0]);
        for (int i=1; i<legalPackageNames.length; i++) {
            sb.append(PACKAGE_NAME_DELIMITER).append(legalPackageNames[i]);
        }
        return sb.toString();
    }
    
	/**
	 * Evaluates the names of the items against the allowable name prefixes. If one or more
	 * do not have valid prefixes then an exception will be thrown.
	 * 
	 * @param item
	 * @param legalNamePrefixes
	 * @throws IllegalArgumentException
	 */
	private void checkValidPackages(Object item, String[] legalNamePrefixes) {
	    Object[] items;
	    if (item.getClass().isArray()) {
		items = (Object[])item;
	    } else{
		items = new Object[]{item};
	    }
		
		String[] names = new String[items.length];
		Set<String> nameSet = new HashSet<String>(items.length);
		String name = null;
		
		for (int i=0; i<items.length; i++) {
			name = packageNameOf(items[i]);
			names[i] = name;
			nameSet.add(name);
		}

		for (int i=0; i<names.length; i++) {
			for (int l=0; l<legalNamePrefixes.length; l++) {
				if (names[i].startsWith(legalNamePrefixes[l])) {
					nameSet.remove(names[i]);
					break;
				}
			}
		}
		if (nameSet.isEmpty()) { return; }
		
		throw new IllegalArgumentException("Invalid items: " + nameSet);
	}
	
	/**
	 * Method itemTypeName.
	 * @return String
	 */
	abstract protected String itemTypeName();
	
	/**
	 *
	 * @param value Object
	 * @return String
	 */
	protected String valueErrorFor(Object value) {
		
		if (value == null) {
			String err = super.valueErrorFor(null);
			if (err != null) { return err; }
			}
		
		if (legalPackageNames == null) {
			return null;	// no restriction
		}
		
		String name = packageNameOf(value);
		
		for (int i=0; i<legalPackageNames.length; i++) {
			if (name.startsWith(legalPackageNames[i])) {
				return null;
			}
		}
		
		return "Disallowed " + itemTypeName() + ": " + name;
	}
	
	/**
	 *
	 * @param item Object
	 * @return String
	 */
	abstract protected String packageNameOf(Object item);
	
	/**
	 *
	 * @return String[]
	 */
	public String[] legalPackageNames() {
		return legalPackageNames;
	}
	
}
