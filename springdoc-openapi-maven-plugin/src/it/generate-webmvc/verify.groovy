//
// Post-build hook for the maven-invoker-plugin 'generate-webmvc' IT.
// Verifies that the plugin produced target/docs/openapi.json with the expected paths.
//
def spec = new File(basedir, 'target/docs/openapi.json')
if (!spec.isFile()) {
    throw new FileNotFoundException('Expected generated OpenAPI at ' + spec)
}

def content = spec.text
if (!content.contains('/pets') || !content.contains('/pets/{id}')) {
    throw new IllegalStateException('Generated OpenAPI is missing the /pets paths: ' + content)
}

println 'Verified WebMvc OpenAPI spec: ' + spec