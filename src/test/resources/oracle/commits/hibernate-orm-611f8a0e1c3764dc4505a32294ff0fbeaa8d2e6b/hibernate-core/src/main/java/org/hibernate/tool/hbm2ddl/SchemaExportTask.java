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
 *
 */
package org.hibernate.tool.hbm2ddl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.collections.ArrayHelper;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;

/**
 * An Ant task for <tt>SchemaExport</tt>.
 *
 * <pre>
 * &lt;taskdef name="schemaexport"
 *     classname="org.hibernate.tool.hbm2ddl.SchemaExportTask"
 *     classpathref="class.path"/&gt;
 *
 * &lt;schemaexport
 *     properties="${build.classes.dir}/hibernate.properties"
 *     quiet="no"
 *     text="no"
 *     drop="no"
 *     delimiter=";"
 *     output="${build.dir}/schema-export.sql"&gt;
 *     &lt;fileset dir="${build.classes.dir}"&gt;
 *         &lt;include name="*.hbm.xml"/&gt;
 *     &lt;/fileset&gt;
 * &lt;/schemaexport&gt;
 * </pre>
 *
 * @see SchemaExport
 * @author Rong C Ou
 */
public class SchemaExportTask extends MatchingTask {
	private List<FileSet> fileSets = new LinkedList<FileSet>();
	private File propertiesFile;
	private File configurationFile;
	private File outputFile;
	private boolean quiet;
	private boolean text;
	private boolean drop;
	private boolean create;
	private boolean haltOnError;
	private String delimiter;
	private String implicitNamingStrategy;
	private String physicalNamingStrategy;

	@SuppressWarnings("UnusedDeclaration")
	public void addFileset(FileSet set) {
		fileSets.add(set);
	}

	/**
	 * Set a properties file
	 * @param propertiesFile the properties file name
	 */
	public void setProperties(File propertiesFile) {
		if ( !propertiesFile.exists() ) {
			throw new BuildException("Properties file: " + propertiesFile + " does not exist.");
	}

		log("Using properties file " + propertiesFile, Project.MSG_DEBUG);
		this.propertiesFile = propertiesFile;
	}

	/**
	 * Set a <literal>.cfg.xml</literal> file, which will be
	 * loaded as a resource, from the classpath
	 * @param configurationFile the path to the resource
	 */
	public void setConfig(File configurationFile) {
		this.configurationFile = configurationFile;
	}

	/**
	 * Enable "quiet" mode. The schema will not be
	 * written to standard out.
	 * @param quiet true to enable quiet mode
	 */
	@SuppressWarnings("UnusedDeclaration")
	public void setQuiet(boolean quiet) {
		this.quiet = quiet;
	}

	/**
	 * Enable "text-only" mode. The schema will not
	 * be exported to the database.
	 * @param text true to enable text-only mode
	 */
	public void setText(boolean text) {
		this.text = text;
	}

	/**
	 * Enable "drop" mode. Database objects will be
	 * dropped but not recreated.
	 * @param drop true to enable drop mode
	 */
	public void setDrop(boolean drop) {
		this.drop = drop;
	}

	/**
	 * Enable "create" mode. Database objects will be
	 * created but not first dropped.
	 * @param create true to enable create mode
	 */
	public void setCreate(boolean create) {
		this.create = create;
	}

	/**
	 * Set the end of statement delimiter for the generated script
	 * @param delimiter the delimiter
	 */
	@SuppressWarnings("UnusedDeclaration")
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * Set the script output file
	 * @param outputFile the file name
	 */
	public void setOutput(File outputFile) {
		this.outputFile = outputFile;
	}

	/**
	 * @deprecated Use {@link #setImplicitNamingStrategy} or {@link #setPhysicalNamingStrategy}
	 * instead
	 */
	@Deprecated
	public void setNamingStrategy(String namingStrategy) {
		DeprecationLogger.DEPRECATION_LOGGER.logDeprecatedNamingStrategyAntArgument();
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setImplicitNamingStrategy(String implicitNamingStrategy) {
		this.implicitNamingStrategy = implicitNamingStrategy;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setPhysicalNamingStrategy(String physicalNamingStrategy) {
		this.physicalNamingStrategy = physicalNamingStrategy;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setHaltonerror(boolean haltOnError) {
		this.haltOnError = haltOnError;
	}

	/**
	 * Execute the task
	 */
	@Override
	public void execute() throws BuildException {
		try {
			buildSchemaExport().execute( !quiet, !text, drop, create );
		}
		catch (HibernateException e) {
			throw new BuildException("Schema text failed: " + e.getMessage(), e);
		}
		catch (FileNotFoundException e) {
			throw new BuildException("File not found: " + e.getMessage(), e);
		}
		catch (IOException e) {
			throw new BuildException("IOException : " + e.getMessage(), e);
		}
		catch (Exception e) {
			throw new BuildException(e);
		}
	}

	private String[] getFiles() {
		List<String> files = new LinkedList<String>();
		for ( FileSet fileSet : fileSets ) {
			final DirectoryScanner ds = fileSet.getDirectoryScanner( getProject() );
			final String[] dsFiles = ds.getIncludedFiles();
			for ( String dsFileName : dsFiles ) {
				File f = new File( dsFileName );
				if ( !f.isFile() ) {
					f = new File( ds.getBasedir(), dsFileName );
				}

				files.add( f.getAbsolutePath() );
			}
		}

		return ArrayHelper.toStringArray(files);
	}

	private SchemaExport buildSchemaExport() throws Exception {
		final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

		final MetadataSources metadataSources = new MetadataSources( bsr );
		final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder( bsr );

		if ( configurationFile != null ) {
			ssrBuilder.configure( configurationFile );
		}
		if ( propertiesFile != null ) {
			ssrBuilder.loadProperties( propertiesFile );
		}
		ssrBuilder.applySettings( getProject().getProperties() );

		for ( String fileName : getFiles() ) {
			if ( fileName.endsWith(".jar") ) {
				metadataSources.addJar( new File( fileName ) );
			}
			else {
				metadataSources.addFile( fileName );
			}
		}


		final StandardServiceRegistryImpl ssr = (StandardServiceRegistryImpl) ssrBuilder.build();
		final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder( ssr );

		ClassLoaderService classLoaderService = bsr.getService( ClassLoaderService.class );
		if ( implicitNamingStrategy != null ) {
			metadataBuilder.applyImplicitNamingStrategy(
					(ImplicitNamingStrategy) classLoaderService.classForName( implicitNamingStrategy ).newInstance()
			);
		}
		if ( physicalNamingStrategy != null ) {
			metadataBuilder.applyPhysicalNamingStrategy(
					(PhysicalNamingStrategy) classLoaderService.classForName( physicalNamingStrategy ).newInstance()
			);
		}

		return new SchemaExport( (MetadataImplementor) metadataBuilder.build() )
				.setHaltOnError( haltOnError )
				.setOutputFile( outputFile.getPath() )
				.setDelimiter( delimiter );
	}

}
