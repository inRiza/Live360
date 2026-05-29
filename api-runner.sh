#!/bin/bash

SPEC="openapi.yaml"
JAVA_ROOT="app/src/main/java"
BASE_PACKAGE="com.example.nimons360.data.remote.dto"
BASE_PATH="${JAVA_ROOT}/${BASE_PACKAGE//./\/}"

GENERATOR_JAR="openapi-generator-cli.jar"
GENERATOR_VERSION="7.2.0"

if [ ! -f "$GENERATOR_JAR" ]; then
    curl -L "https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/$GENERATOR_VERSION/openapi-generator-cli-$GENERATOR_VERSION.jar" -o "$GENERATOR_JAR"
fi

rm -rf "$BASE_PATH"
mkdir -p "$BASE_PATH/request"
mkdir -p "$BASE_PATH/response"
mkdir -p "$BASE_PATH/common"

rm -rf tmp_gen
# Added --type-mappings=URI=String to prevent URI mismatch errors
# Added --request旜dy-for-multipart for multipart form data support
java -jar "$GENERATOR_JAR" generate \
    -i "$SPEC" \
    -g kotlin \
    -o tmp_gen \
    --type-mappings=URI=String \
    --global-property models \
    --additional-properties=modelPackage=$BASE_PACKAGE,enumPropertyNaming=UPPERCASE,serializationLibrary=gson

GEN_FILES="tmp_gen/src/main/kotlin/${BASE_PACKAGE//./\/}"

for file in "$GEN_FILES"/*.kt; do
    [ -e "$file" ] || continue
    filename=$(basename "$file")

    if [[ "$filename" == *Request.kt ]]; then
        TARGET_DIR="$BASE_PATH/request"
        TARGET_PKG="$BASE_PACKAGE.request"
    elif [[ "$filename" == *Response.kt ]]; then
        TARGET_DIR="$BASE_PATH/response"
        TARGET_PKG="$BASE_PACKAGE.response"
    else
        TARGET_DIR="$BASE_PATH/common"
        TARGET_PKG="$BASE_PACKAGE.common"
    fi

    sed -i "s/package $BASE_PACKAGE/package $TARGET_PKG/g" "$file"

    sed -i "/import $BASE_PACKAGE/d" "$file"

    sed -i "/package $TARGET_PKG/a \\
\\
import $BASE_PACKAGE.request.*\\
import $BASE_PACKAGE.response.*\\
import $BASE_PACKAGE.common.*" "$file"

    mv "$file" "$TARGET_DIR/"
done

rm -rf tmp_gen
