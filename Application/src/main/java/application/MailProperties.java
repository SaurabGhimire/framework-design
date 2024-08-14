package application;

import framework.annotations.ConfigurationProperties;

@ConfigurationProperties(prefix = "myapp.mail")
public class MailProperties {
    private String to;
    private String host;
}
