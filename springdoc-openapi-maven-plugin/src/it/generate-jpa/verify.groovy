//
// Post-build hook for the maven-invoker-plugin 'generate-jpa' IT.
// Verifies the plugin generated a spec for an app whose context needs a real DataSource
// (spring-boot-starter-data-jpa + Hibernate), using the spring.datasource.* overrides passed
// through systemProperties. It also checks the JPA @Entity was reflected as a schema.
//
def spec = new File(basedir, 'target/docs/openapi.json')
if (!spec.isFile()) {
    throw new FileNotFoundException('Expected generated OpenAPI at ' + spec)
}

def content = spec.text
if (!content.contains('/pets') || !content.contains('/pets/{id}')) {
    throw new IllegalStateException('Generated OpenAPI is missing the /pets paths: ' + content)
}
// The Pet entity should be reflected as a schema (proves JPA annotations were read).
if (!content.contains('"Pet"')) {
    throw new IllegalStateException('Generated OpenAPI is missing the JPA Pet schema: ' + content)
}

println 'Verified JPA (Hibernate + DataSource override) OpenAPI spec: ' + spec