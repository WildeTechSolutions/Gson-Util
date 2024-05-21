module com.wildetechsolutions.gson {
    requires org.slf4j;
    requires java.persistence;
    requires com.google.gson;
    requires org.apache.commons.lang3;
    requires commons.beanutils;

    requires com.wildetechsolutions.reflection;

    exports com.wildetechsolutions.gson;
}