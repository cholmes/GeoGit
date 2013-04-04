/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.geotools.cli.test.functional;

import org.geogit.cli.test.functional.AbstractGeogitFunctionalTest;

public abstract class AbstractGeoToolsFunctionalTest extends AbstractGeogitFunctionalTest {

    protected String getPGDatabaseParameters() throws Exception {
        IniPGProperties properties = new IniPGProperties();
        StringBuilder sb = new StringBuilder();
        sb.append(" --host ");
        sb.append(properties.get("database.host", String.class).or("localhost"));

        sb.append(" --port ");
        sb.append(properties.get("database.port", String.class).or("5432"));

        sb.append(" --schema ");
        sb.append(properties.get("database.schema", String.class).or("public"));

        sb.append(" --database ");
        sb.append(properties.get("database.database", String.class).or("database"));

        sb.append(" --user ");
        sb.append(properties.get("database.user", String.class).or("postgres"));

        sb.append(" --password ");
        sb.append(properties.get("database.password", String.class).or("postgres"));

        return sb.toString();
    }
}
