package uk.co.autotrader.traverson.conversion;

import com.alibaba.fastjson.JSONObject;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.co.autotrader.traverson.exception.ConversionException;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResourceConversionServiceTest {

    private ResourceConversionService service = ResourceConversionService.getInstance();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Mock
    private ResourceConverter<Number> converter;
    @Mock
    private ResourceConverter<Object> failingConverter;

    @Test
    public void init_EnsuresThatTheDefaultConvertersAreRegistered() throws Exception {
        Map<Class<?>, ResourceConverter<?>> converters = service.getConvertersByClass();

        assertThat(converters).isNotEmpty();
        assertThat(converters.values()).extracting("class").contains(FastJsonResourceConverter.class, StringResourceConverter.class);
    }

    @Test
    public void convert_GivenUnsupportedResponseType_ThrowsException() throws Exception {
        expectedException.expect(ConversionException.class);
        expectedException.expectCause(CoreMatchers.nullValue(Throwable.class));
        expectedException.expectMessage("Unsupported return type of uk.co.autotrader.traverson.conversion.UnsupportedType");

        service.convert("{}", UnsupportedType.class);
    }

    @Test
    public void addConverter_RegistersTheConverterForUseLater() throws Exception {

        SupportedType result = service.convert("Test value", SupportedType.class);

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo("Test value");
    }

    @Test
    public void convert_GivenRequestForFastJSON_EnsuresTheFastJsonConverterIsLoaded() throws Exception {
        JSONObject resource = service.convert("{'name':'test'}", JSONObject.class);

        assertThat(resource).isNotNull().containsEntry("name", "test");
    }

    @Test
    public void convert_GivenRequestForString_EnsuresTheStringConverterIsLoaded() throws Exception {
        String resource = service.convert("{'name':'test'}", String.class);

        assertThat(resource).isNotNull().isEqualTo("{'name':'test'}");
    }

    @Test
    public void convert_GivenTheConvertersAreLoadedInAnyOrder_TheConversionServiceWillTraverseTheClassHierarchyUntilAMatch() throws Exception {
        when(failingConverter.getDestinationType()).thenReturn(Object.class);
        when(converter.getDestinationType()).thenReturn(Number.class);
        when(converter.convert("1234", Integer.class)).thenReturn(1234);
        Map<Class<?>, ResourceConverter<?>> converters = new LinkedHashMap<Class<?>, ResourceConverter<?>>();
        converters.put(failingConverter.getDestinationType(), failingConverter);
        converters.put(converter.getDestinationType(), converter);
        ResourceConversionService service = new ResourceConversionService(converters);

        Integer value = service.convert("1234", Integer.class);

        assertThat(value).isEqualTo(1234);
    }

}
