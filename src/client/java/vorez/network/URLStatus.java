package vorez.network;

public enum URLStatus {
    // fails
    FAIL,
    OFFLINE,
    NO_RESPONSE,
    INVALID_URL,
    ERROR_404,
    UNSTABLE_CONNECTION,
    HTTP_DENIED,
    NO_INTERNET,
    NO_ACCESS,
    DOMAIN_NOT_FOUND,

    // success
    SUCCESS,

    // default
    URL_EMPTY,
    IS_EXAMPLE_COM,
    CUSTOM_SERVER_DISABLED,
    IS_RAW_GITHUB,
}
