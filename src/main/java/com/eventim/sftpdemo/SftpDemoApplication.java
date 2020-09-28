package com.eventim.sftpdemo;

import com.jcraft.jsch.ChannelSftp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.support.GenericMessage;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class SftpDemoApplication {

    // Adjust these values to point at a valid SFTP server
    private static final String HOST = "localhost";
    private static final int PORT = 2222;
    private static final String USER = "foo";
    private static final String PASS = "pass";

    // Ensure this directory is writable on the SFTP server by the above user
    private static final String DIRECTORY = "/DE/";

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    public static void main(String[] args) {
        SpringApplication.run(SftpDemoApplication.class, args);
    }

    @Bean
    public SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory() {
        DefaultSftpSessionFactory sf = new DefaultSftpSessionFactory();
        sf.setHost(HOST);
        sf.setPort(PORT);
        sf.setUser(USER);
        sf.setPassword(PASS);
        sf.setAllowUnknownKeys(true);
        return new CachingSessionFactory<>(sf);
    }

    @Bean
    public MessageSource<String> source() {
        return () -> new GenericMessage<>("Hello World!");
    }

    @Bean
    public IntegrationFlow flow() {
        return IntegrationFlows.from(source(), c -> c.poller(Pollers.fixedDelay(Duration.ofSeconds(10))))
                .handle(Sftp.outboundGateway(sftpSessionFactory(),
                        AbstractRemoteFileOutboundGateway.Command.PUT,
                        "payload")
                        .fileNameGenerator(g -> String.valueOf(atomicInteger.getAndIncrement()))
                        .remoteDirectoryFunction(m -> DIRECTORY))
                .nullChannel();
    }

}
