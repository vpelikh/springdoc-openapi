//
// Post-build hook for the maven-invoker-plugin 'generate-yaml' IT.
// Verifies that <format>yaml</format> produces target/docs/openapi.yaml (not .json)
// containing the expected paths and a YAML-ish body.
//
def spec = new File(basedir, 'target/docs/openapi.yaml')
if (!spec.isFile()) {
    throw new FileNotFoundException('Expected generated OpenAPI YAML at ' + spec)
}

def content = spec.text
if (!content.contains('openapi:')) {
    throw new IllegalStateException('Generated file does not look like OpenAPI YAML: ' + content)
}
if (!content.contains('/pets') || !content.contains('/pets/{id}')) {
    throw new IllegalStateException('Generated OpenAPI YAML is missing the /pets paths: ' + content)
}

// A JSON file would start with '{' or '['; a YAML spec should not.
if (content.trim().startsWith('{') || content.contains('"openapi"')) {
    throw new IllegalStateException('Expected YAML output but found JSON instead: ' + content)
}

println 'Verified OpenAPI YAML spec: ' + spec