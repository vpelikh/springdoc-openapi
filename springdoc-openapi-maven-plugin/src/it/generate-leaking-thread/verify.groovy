//
// Post-build hook for the maven-invoker-plugin 'generate-leaking-thread' IT.
// The sample app starts a non-daemon thread that keeps the worker JVM alive on natural exit;
// the plugin/worker must still terminate and produce target/docs/openapi.json with the expected
// paths, otherwise this build would hang until the invoker timeouts.
//
def spec = new File(basedir, 'target/docs/openapi.json')
if (!spec.isFile()) {
    throw new FileNotFoundException('Expected generated OpenAPI at ' + spec)
}

def content = spec.text
if (!content.contains('/pets') || !content.contains('/pets/{id}')) {
    throw new IllegalStateException('Generated OpenAPI is missing the /pets paths: ' + content)
}

println 'Verified OpenAPI spec: ' + spec