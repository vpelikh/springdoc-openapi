//
// Post-build hook for the maven-invoker-plugin 'generate-skip' IT.
// Verifies that setting <skip>true</skip> skips generation entirely: no spec document
// is produced and the build (which still runs the bound goal on the package phase)
// succeeds rather than requiring mainClass/context boot.
//
def docsDir = new File(basedir, 'target/docs')
if (docsDir.exists()) {
    def leftovers = docsDir.listFiles()
    if (leftovers != null && leftovers.length > 0) {
        throw new IllegalStateException('Expected no generated documents when skip=true but found: '
                + leftovers*.name.join(', '))
    }
}

println 'Verified: skip=true produced no OpenAPI document'