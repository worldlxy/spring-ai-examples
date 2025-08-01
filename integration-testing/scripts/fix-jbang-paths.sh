#!/bin/bash

# Fix all JBang script paths based on actual directory depth

echo "🔧 Fixing JBang script paths..."

# 3 levels up (most modules)
find /home/mark/projects/spring-ai-examples -name "Run*.java" -path "*/integration-tests/*" | while read script; do
    dir=$(dirname $(dirname "$script"))
    if [[ -d "$dir/../../integration-testing" ]]; then
        echo "  Fixing 3-level path: $script"
        sed -i 's|//SOURCES ../../../../integration-testing/jbang-lib/IntegrationTestUtils.java|//SOURCES ../../../integration-testing/jbang-lib/IntegrationTestUtils.java|g' "$script"
        sed -i 's|//SOURCES ../../../../../integration-testing/jbang-lib/IntegrationTestUtils.java|//SOURCES ../../../integration-testing/jbang-lib/IntegrationTestUtils.java|g' "$script"
    fi
done

# 4 levels up (models/chat/helloworld)
script="/home/mark/projects/spring-ai-examples/models/chat/helloworld/integration-tests/RunHelloworld.java"
if [[ -f "$script" ]]; then
    echo "  Fixing 4-level path: $script"
    sed -i 's|//SOURCES ../../../integration-testing/jbang-lib/IntegrationTestUtils.java|//SOURCES ../../../../integration-testing/jbang-lib/IntegrationTestUtils.java|g' "$script"
fi

# Check for any 5-level deep modules
find /home/mark/projects/spring-ai-examples -name "Run*.java" -path "*/integration-tests/*" | while read script; do
    dir=$(dirname $(dirname "$script"))
    if [[ ! -d "$dir/../../integration-testing" ]] && [[ ! -d "$dir/../../../integration-testing" ]] && [[ -d "$dir/../../../../integration-testing" ]]; then
        echo "  Fixing 5-level path: $script"
        sed -i 's|//SOURCES ../../../integration-testing/jbang-lib/IntegrationTestUtils.java|//SOURCES ../../../../../integration-testing/jbang-lib/IntegrationTestUtils.java|g' "$script"
        sed -i 's|//SOURCES ../../../../integration-testing/jbang-lib/IntegrationTestUtils.java|//SOURCES ../../../../../integration-testing/jbang-lib/IntegrationTestUtils.java|g' "$script"
    fi
done

echo "✅ Path fixes complete!"