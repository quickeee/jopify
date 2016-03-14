package com.feedeo.shopify.web.resource.rest;

import com.feedeo.shopify.exception.web.resource.rest.NotFoundRestResourceException;
import com.feedeo.shopify.exception.web.resource.rest.UnauthorizedAccessRestResourceException;
import com.feedeo.shopify.model.Shop;
import com.feedeo.shopify.web.resource.rest.shop.ShopOAuth2RestResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.MockRestServiceServer.createServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

@RunWith(MockitoJUnitRunner.class)
public class ShopifyOAuth2RestResourceTest {

    private ShopifyOAuth2RestResource target;

    private MockRestServiceServer mockServer;

    @Before
    public void setUp() {
        target = new ShopOAuth2RestResource();

        RestTemplate restTemplate = (RestTemplate) target.getRestOperations();

        mockServer = createServer(restTemplate);
    }

    @Test
    public void shouldFailWithNotFoundResourceException() {
        final String url = "https://my-shop.myshopify.com/admin/shop.json";

        mockServer
                .expect(requestTo(url))
                .andExpect(method(GET))
                .andRespond(ShopifyMockRestResponseCreators.withNotFound());

        Throwable throwable = null;
        try {
            target.getRestOperationsWithAccessToken("my-access-token")
                    .getForEntity(url, Shop.class, "my-shop-name")
                    .getBody();
        } catch (Throwable e) {
            throwable = e;
        } finally {
            assertThat(throwable)
                    .hasCauseExactlyInstanceOf(NotFoundRestResourceException.class);
        }

        mockServer.verify();
    }

    @Test
    public void shouldFailWithUnauthorizedAccessResourceException() {
        final String url = "https://my-shop.myshopify.com/admin/shop.json";

        mockServer
                .expect(requestTo(url))
                .andExpect(method(GET))
                .andRespond(ShopifyMockRestResponseCreators.withUnauthorized());

        Throwable throwable = null;
        try {
            target.getRestOperationsWithAccessToken("my-access-token")
                    .getForEntity(url, Shop.class, "my-shop-name")
                    .getBody();
        } catch (Throwable e) {
            throwable = e;
        } finally {
            assertThat(throwable)
                    .hasCauseExactlyInstanceOf(UnauthorizedAccessRestResourceException.class);
        }

        mockServer.verify();
    }

    private abstract static class ShopifyMockRestResponseCreators {

        private static final String BODY_NOT_FOUND = "{\"errors\": \"Not Found\"}";
        private static final String BODY_UNAUTHORIZED = "{\"errors\": \"[API] Invalid API key or access token (unrecognized login or wrong password)\"}";

        public static DefaultResponseCreator withNotFound() {
            return new ShopifyResponseCreator(NOT_FOUND).body(BODY_NOT_FOUND).contentType(APPLICATION_JSON);
        }


        public static DefaultResponseCreator withUnauthorized() {
            return new ShopifyResponseCreator(UNAUTHORIZED).body(BODY_UNAUTHORIZED).contentType(APPLICATION_JSON);
        }
    }

    /**
     * Because {@code MockRestResponseCreators} is limited and {@code DefaultResponseCreator} constructor is protected.
     */
    private static class ShopifyResponseCreator extends DefaultResponseCreator {

        protected ShopifyResponseCreator(HttpStatus statusCode) {
            super(statusCode);
        }
    }
}
