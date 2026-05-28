/*-
 * #%L
 * Serve MCP
 * %%
 * Copyright (C) 2026 Reed von Redwitz
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package build.serve.mcp;

import build.base.json.Json;
import build.base.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolParamTest {

    // --- Base behavior (tested once via StringParam as the representative type) ---

    @Test
    void shouldThrowWhenRequiredParamMissing() {
        final var param = ToolParam.string("city", "City name");
        assertThatThrownBy(() -> param.extract(Json.parse("{}")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("city");
    }

    @Test
    void shouldThrowWhenRequiredParamNull() {
        final var param = ToolParam.string("city", "City name");
        assertThatThrownBy(() -> param.extract(Json.parse("{\"city\":null}")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("city");
    }

    @Test
    void shouldReturnNullWhenOptionalParamMissing() {
        assertThat(ToolParam.string("city", "City name").optional().extract(Json.parse("{}"))).isNull();
    }

    @Test
    void shouldReturnNullWhenOptionalParamNull() {
        assertThat(ToolParam.string("city", "City name").optional().extract(Json.parse("{\"city\":null}"))).isNull();
    }

    @Test
    void shouldReturnDefaultWhenOptionalParamMissing() {
        assertThat(ToolParam.string("city", "City name").optional("Paris").extract(Json.parse("{}"))).isEqualTo("Paris");
    }

    @Test
    void shouldReturnDefaultWhenOptionalParamNull() {
        assertThat(ToolParam.string("city", "City name").optional("Paris").extract(Json.parse("{\"city\":null}"))).isEqualTo("Paris");
    }

    @Test
    void shouldReturnValueWhenOptionalParamPresent() {
        assertThat(ToolParam.string("city", "City name").optional("Paris").extract(Json.parse("{\"city\":\"Berlin\"}"))).isEqualTo("Berlin");
    }

    @Test
    void shouldReportRequiredForNewParam() {
        assertThat(ToolParam.string("x", "x").isRequired()).isTrue();
    }

    @Test
    void shouldNotBeRequiredAfterOptional() {
        assertThat(ToolParam.string("x", "x").optional().isRequired()).isFalse();
    }

    // --- Factory return types ---

    @Test
    void shouldReturnStringParamFromStringFactory() {
        assertThat(ToolParam.string("x", "x")).isInstanceOf(StringParam.class);
    }

    @Test
    void shouldReturnNumberParamIntegerFromIntegerFactory() {
        final NumberParam<Integer> param = ToolParam.integer("x", "x");
        assertThat(param).isInstanceOf(NumberParam.class);
    }

    @Test
    void shouldReturnNumberParamDoubleFromNumberFactory() {
        final NumberParam<Double> param = ToolParam.number("x", "x");
        assertThat(param).isInstanceOf(NumberParam.class);
    }

    @Test
    void shouldReturnBoolParamFromBoolFactory() {
        assertThat(ToolParam.bool("x", "x")).isInstanceOf(BoolParam.class);
    }

    @Test
    void shouldReturnArrayParamFromUntypedArrayFactory() {
        assertThat(ToolParam.array("x", "x")).isInstanceOf(ArrayParam.class);
    }

    @Test
    void shouldReturnArrayParamFromTypedArrayFactory() {
        assertThat(ToolParam.array("x", "x", ToolParam.string("item", "item"))).isInstanceOf(ArrayParam.class);
    }

    @Test
    void shouldReturnObjectParamFromObjectFactory() {
        assertThat(ToolParam.object("x", "x", List.of())).isInstanceOf(ObjectParam.class);
    }

    @Test
    void shouldReturnStringParamFromValuesMethod() {
        assertThat(ToolParam.string("x", "x").values(List.of("a"))).isInstanceOf(StringParam.class);
    }

    @Test
    void shouldPreserveSpecificTypeAfterOptional() {
        assertThat(ToolParam.string("x", "x").optional()).isInstanceOf(StringParam.class);
        assertThat(ToolParam.integer("x", "x").optional()).isInstanceOf(NumberParam.class);
        assertThat(ToolParam.bool("x", "x").optional()).isInstanceOf(BoolParam.class);
    }

    // --- Per-type: extraction value and schema content ---

    @Test
    void shouldExtractStringValue() {
        assertThat(ToolParam.string("city", "City name").extract(Json.parse("{\"city\":\"London\"}"))).isEqualTo("London");
    }

    @Test
    void shouldProduceStringSchema() {
        final var schema = ToolParam.string("city", "City name").propertySchema();
        assertThat(schema.getString("type")).isEqualTo("string");
        assertThat(schema.getString("description")).isEqualTo("City name");
    }

    @Test
    void shouldExtractIntegerValue() {
        assertThat(ToolParam.integer("count", "Count").extract(Json.parse("{\"count\":42}"))).isEqualTo(42);
    }

    @Test
    void shouldProduceIntegerSchema() {
        final var schema = ToolParam.integer("count", "Count").propertySchema();
        assertThat(schema.getString("type")).isEqualTo("integer");
    }

    @Test
    void shouldExtractDoubleValue() {
        assertThat(ToolParam.number("tempo", "BPM").extract(Json.parse("{\"tempo\":120.5}"))).isEqualTo(120.5);
    }

    @Test
    void shouldProduceNumberSchema() {
        final var schema = ToolParam.number("tempo", "BPM").propertySchema();
        assertThat(schema.getString("type")).isEqualTo("number");
    }

    @Test
    void shouldExtractBoolTrue() {
        assertThat(ToolParam.bool("shuffle", "Shuffle").extract(Json.parse("{\"shuffle\":true}"))).isTrue();
    }

    @Test
    void shouldExtractBoolFalse() {
        assertThat(ToolParam.bool("shuffle", "Shuffle").extract(Json.parse("{\"shuffle\":false}"))).isFalse();
    }

    @Test
    void shouldProduceBooleanSchema() {
        final var schema = ToolParam.bool("shuffle", "Shuffle").propertySchema();
        assertThat(schema.getString("type")).isEqualTo("boolean");
    }

    @Test
    void shouldExtractUntypedArrayAsList() {
        final var result = ToolParam.array("steps", "Steps").extract(Json.parse("{\"steps\":[\"a\",\"b\"]}"));
        assertThat(result).hasSize(2);
        assertThat(result.get(0).asString().value()).isEqualTo("a");
    }

    @Test
    void shouldProduceArraySchema() {
        final var schema = ToolParam.array("steps", "Steps").propertySchema();
        assertThat(schema.getString("type")).isEqualTo("array");
    }

    @Test
    void shouldExtractValueInValuesList() {
        assertThat(ToolParam.string("dir", "Direction").values(List.of("up", "down")).extract(Json.parse("{\"dir\":\"up\"}"))).isEqualTo("up");
    }

    @Test
    void shouldThrowWhenValueNotInValuesList() {
        final var param = ToolParam.string("dir", "Direction").values(List.of("up", "down"));
        assertThatThrownBy(() -> param.extract(Json.parse("{\"dir\":\"left\"}")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dir");
    }

    @Test
    void shouldProduceEnumSchemaFromValues() {
        final var schema = ToolParam.string("dir", "Direction").values(List.of("up", "down")).propertySchema();
        assertThat(schema.has("enum")).isTrue();
        final var enumArr = schema.get("enum").asArray();
        assertThat(enumArr.element(0).asString().value()).isEqualTo("up");
        assertThat(enumArr.element(1).asString().value()).isEqualTo("down");
    }

    // --- Composable: object ---

    private static final StringParam STEP_TYPE = ToolParam.string("type", "Step type").values(List.of("seed", "filter", "terminal"));
    private static final ToolParam<String> STEP_ID = ToolParam.string("id", "Step ID");
    private static final ObjectParam STEP = ToolParam.object("step", "A pipeline step", List.of(STEP_TYPE, STEP_ID));

    @Test
    void shouldExtractNestedObjectFields() {
        final var step = STEP.extract(Json.parse("{\"step\":{\"type\":\"seed\",\"id\":\"notes\"}}"));
        assertThat(STEP_TYPE.extract(step)).isEqualTo("seed");
        assertThat(STEP_ID.extract(step)).isEqualTo("notes");
    }

    @Test
    void shouldProduceObjectSchemaWithPropertiesAndRequired() {
        final var schema = STEP.propertySchema();
        assertThat(schema.getString("type")).isEqualTo("object");
        assertThat(schema.get("properties").asObject().has("type")).isTrue();
        assertThat(schema.get("properties").asObject().has("id")).isTrue();
        assertThat(schema.get("required").asArray().values()).hasSize(2);
    }

    @Test
    void shouldThrowWhenObjectRequiredFieldMissing() {
        assertThatThrownBy(() -> STEP.extract(Json.parse("{\"step\":{\"type\":\"seed\"}}")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id");
    }

    @Test
    void shouldThrowWhenObjectFieldValueNotInAllowedList() {
        assertThatThrownBy(() -> STEP.extract(Json.parse("{\"step\":{\"type\":\"bogus\",\"id\":\"x\"}}")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("type");
    }

    // --- Composable: typed array (pipeline) ---

    private static final ArrayParam<JsonObject> PIPELINE = ToolParam.array("pipeline", "Ordered steps", STEP);

    @Test
    void shouldExtractTypedArrayItems() {
        final var steps = PIPELINE.extract(Json.parse("""
            {"pipeline":[
                {"type":"seed","id":"notes"},
                {"type":"terminal","id":"collect"}
            ]}"""));
        assertThat(steps).hasSize(2);
        assertThat(STEP_TYPE.extract(steps.get(0))).isEqualTo("seed");
        assertThat(STEP_TYPE.extract(steps.get(1))).isEqualTo("terminal");
    }

    @Test
    void shouldProduceTypedArraySchemaWithItems() {
        final var schema = PIPELINE.propertySchema();
        assertThat(schema.getString("type")).isEqualTo("array");
        assertThat(schema.get("items").asObject().getString("type")).isEqualTo("object");
        assertThat(schema.get("items").asObject().get("properties").asObject().has("type")).isTrue();
    }

    @Test
    void shouldThrowWhenArrayItemMissingRequiredField() {
        assertThatThrownBy(() -> PIPELINE.extract(Json.parse("""
            {"pipeline":[
                {"type":"seed","id":"notes"},
                {"type":"filter"}
            ]}""")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id");
    }

    // --- of() escape hatch ---

    @Test
    void shouldExtractValueWithCustomSchemaAndExtractor() {
        final var schema = JsonObject.builder().put("type", "object").put("description", "Pipeline").build();
        final var param = ToolParam.of("pipeline", "Pipeline", schema, val -> val.asObject());
        final var result = param.extract(Json.parse("{\"pipeline\":{\"seed\":\"notes\"}}"));
        assertThat(result.getString("seed")).isEqualTo("notes");
    }

    @Test
    void shouldPassThroughCustomSchema() {
        final var schema = JsonObject.builder().put("type", "object").put("description", "Custom").build();
        assertThat(ToolParam.of("p", "Custom", schema, val -> val).propertySchema().getString("type")).isEqualTo("object");
    }

    // --- StringParam.minLength / maxLength ---

    @Test
    void shouldThrowWhenStringTooShort() {
        final var param = ToolParam.string("code", "Code").minLength(3);
        assertThatThrownBy(() -> param.extract(Json.parse("{\"code\":\"ab\"}")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("code");
    }

    @Test
    void shouldPassWhenStringAtMinLength() {
        assertThat(ToolParam.string("code", "Code").minLength(3).extract(Json.parse("{\"code\":\"abc\"}"))).isEqualTo("abc");
    }

    @Test
    void shouldThrowWhenStringTooLong() {
        final var param = ToolParam.string("code", "Code").maxLength(3);
        assertThatThrownBy(() -> param.extract(Json.parse("{\"code\":\"abcd\"}")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("code");
    }

    @Test
    void shouldProduceMinLengthInSchema() {
        final var schema = ToolParam.string("code", "Code").minLength(3).propertySchema();
        assertThat(schema.get("minLength").asNumber().toNumber().intValue()).isEqualTo(3);
    }

    @Test
    void shouldAccumulateSchemaWhenChainingStringConstraints() {
        final var schema = ToolParam.string("code", "Code").minLength(2).maxLength(10).propertySchema();
        assertThat(schema.get("minLength").asNumber().toNumber().intValue()).isEqualTo(2);
        assertThat(schema.get("maxLength").asNumber().toNumber().intValue()).isEqualTo(10);
    }

    // --- refine ---

    @Test
    void shouldReturnStringParamFromRefine() {
        assertThat(ToolParam.string("code", "Code").refine(s -> true, "")).isInstanceOf(StringParam.class);
    }

    @Test
    void shouldExtractWhenRefinePredicatePasses() {
        final var param = ToolParam.string("code", "Code").refine(s -> s.startsWith("A"), "must start with A");
        assertThat(param.extract(Json.parse("{\"code\":\"Alpha\"}"))).isEqualTo("Alpha");
    }

    @Test
    void shouldThrowWhenRefinePredicateFails() {
        final var param = ToolParam.string("code", "Code").refine(s -> s.startsWith("A"), "must start with A");
        assertThatThrownBy(() -> param.extract(Json.parse("{\"code\":\"Beta\"}")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must start with A");
    }

    // --- safeParse ---

    @Test
    void shouldReturnEmptyFromSafeParseWhenMissing() {
        assertThat(ToolParam.string("x", "x").safeParse(Json.parse("{}"))).isEmpty();
    }

    @Test
    void shouldReturnValueFromSafeParseWhenPresent() {
        assertThat(ToolParam.string("x", "x").safeParse(Json.parse("{\"x\":\"hello\"}"))).contains("hello");
    }

    // --- NumberParam.min / max ---

    @Test
    void shouldThrowWhenNumberBelowMin() {
        final var param = ToolParam.integer("count", "Count").min(1);
        assertThatThrownBy(() -> param.extract(Json.parse("{\"count\":0}")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("count");
    }

    @Test
    void shouldPassWhenNumberAtMin() {
        assertThat(ToolParam.integer("count", "Count").min(1).extract(Json.parse("{\"count\":1}"))).isEqualTo(1);
    }

    @Test
    void shouldThrowWhenNumberAboveMax() {
        final var param = ToolParam.integer("count", "Count").max(10);
        assertThatThrownBy(() -> param.extract(Json.parse("{\"count\":11}")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("count");
    }

    @Test
    void shouldProduceMinimumInSchema() {
        final var schema = ToolParam.number("tempo", "BPM").min(60.0).propertySchema();
        assertThat(schema.get("minimum").asNumber().toNumber().doubleValue()).isEqualTo(60.0);
    }

    @Test
    void shouldAccumulateSchemaWhenChainingNumberConstraints() {
        final var schema = ToolParam.number("tempo", "BPM").min(20.0).max(300.0).propertySchema();
        assertThat(schema.get("minimum").asNumber().toNumber().doubleValue()).isEqualTo(20.0);
        assertThat(schema.get("maximum").asNumber().toNumber().doubleValue()).isEqualTo(300.0);
    }

    // --- safeParse with wrong JSON type ---

    @Test
    void shouldReturnEmptyFromSafeParseOnWrongJsonType() {
        final var param = ToolParam.string("city", "City name");
        assertThat(param.safeParse(Json.parse("{\"city\":42}"))).isEmpty();
    }

    // --- ArrayParam.minItems / maxItems ---

    @Test
    void shouldThrowWhenArrayTooShort() {
        final var param = ToolParam.array("tags", "Tags").minItems(2);
        assertThatThrownBy(() -> param.extract(Json.parse("{\"tags\":[\"a\"]}")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tags");
    }

    @Test
    void shouldThrowWhenArrayTooLong() {
        final var param = ToolParam.array("tags", "Tags").maxItems(2);
        assertThatThrownBy(() -> param.extract(Json.parse("{\"tags\":[\"a\",\"b\",\"c\"]}")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tags");
    }

    @Test
    void shouldProduceMinItemsInSchema() {
        final var schema = ToolParam.array("tags", "Tags").minItems(1).propertySchema();
        assertThat(schema.get("minItems").asNumber().toNumber().intValue()).isEqualTo(1);
    }
}
