package com.ipsoft.plugins.bamboo.xmppmuc;

import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.instantmessagingserver.InstantMessagingServerDefinition;
import com.atlassian.bamboo.instantmessagingserver.InstantMessagingServerManager;
import com.atlassian.bamboo.notification.Notification;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smack.java7.Java7SmackInitializer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;

public class XMPPMucNotificationTransport implements NotificationTransport
{
    private static final Logger log = Logger.getLogger(XMPPMucNotificationTransport.class);

    private static final Integer DEFAULT_TLS_PORT = 5222;
    private static final Integer DEFAULT_SSL_PORT = 5223;
    private static final String DEFAULT_RESOURCE = "Bamboo";
    private InstantMessagingServerManager instantMessagingServerManager;
    private XMPPTCPConnection connection;
    private XMPPMucConferenceInstantMessagingServerDefinition imserver;

    private final String room;
    private final String roompw;
    private MultiUserChat muc;

    @Nullable
    private final ImmutablePlan plan;
    @Nullable
    private final ResultsSummary resultsSummary;
    @Nullable
    private final DeploymentResult deploymentResult;

    /**
     *
     * @param room
     * @param roompw
     * @param plan
     * @param resultsSummary
     * @param deploymentResult
     * @param customVariableContext
     * Called by XMPPMucNotificationRecipient getTransports().
     * Takes room name, password, plan, results, etc and sets them into object space
     */
    public XMPPMucNotificationTransport(String room,
                                        String roompw,
                                      @Nullable ImmutablePlan plan,
                                      @Nullable ResultsSummary resultsSummary,
                                      @Nullable DeploymentResult deploymentResult,
                                      CustomVariableContext customVariableContext)
    {
        this.room = customVariableContext.substituteString(room);
        this.roompw = customVariableContext.substituteString(roompw);
        this.plan = plan;
        this.resultsSummary = resultsSummary;
        this.deploymentResult = deploymentResult;
    }

    /**
     *
     * @param notification
     * Takes the notification from Bamboo and attempts to send it via XMPP MUC
     * Will attempt to use existing XMPPConnection if they exist else will create a new one with a call to this.getConnection
     * After getting connection it will join the MUC room and send the message
     */
    public void sendNotification(@NotNull Notification notification)
    {
        String message = notification.getIMContent();
        XMPPMucConferenceInstantMessagingServerDefinition server = this.getMessagingServerDefinition();

        
        if (server == null) {
            log.error("IM Server is not configured");
        } else {            

            if (this.connection != null && this.connection.isConnected()){
                log.info("Using existing connection");
            } else {
                try {
                    this.connection = this.getConnection(server);
                } catch (XMPPException e) {
                    log.info("Unable to get XMPP MUC Connection");
                    log.info(e.getStackTrace());
                }
            }

            //Define username and add unique var to prevent duplicate jids
            String code = RandomStringUtils.randomAlphanumeric(4).toUpperCase();
            String DEFAULT_CHANNEL_USERNAME = "Bamboo Test Agent " + code;

            MultiUserChatManager mucm = MultiUserChatManager.getInstanceFor(this.connection);
            try {
                List<String> services = mucm.getServiceNames();
                if (services == null || services.isEmpty()) {
                    log.info("XMPP MUC no services found, unable to connect to MUC room");
                } else {
                    MultiUserChat muc = mucm.getMultiUserChat(room);
                    this.muc = muc;

                    try {
                        if (roompw != null && !roompw.isEmpty()) {
                            this.muc.join(DEFAULT_CHANNEL_USERNAME, this.roompw);
                        } else {
                            this.muc.join(DEFAULT_CHANNEL_USERNAME);
                        }
                    } catch (XMPPException e){
                        log.info("XMPP MUC Exemption while trying to join room.");
                        log.info(e.getStackTrace());
                    } catch (SmackException e) {
                        log.info("XMPP MUC SmackException while trying to join room.");
                        log.info(e.getStackTrace());
                    }
                    this.muc.sendMessage(message);
                    // Leave the channel when done
                    this.muc.leave();
                }
            } catch (SmackException.NoResponseException e){
                log.info("XMPP MUC no response to query for service names");
                log.trace(e.getStackTrace());
            } catch (XMPPException.XMPPErrorException e){
                log.info("XMPP MUC unknown exception");
                log.trace(e.getStackTrace());
            } catch (SmackException.NotConnectedException e){
                log.info("XMPP MUC not connected to query for service names");
                log.trace(e.getStackTrace());
            }
        }

    }

    public XMPPMucConferenceInstantMessagingServerDefinition getMessagingServerDefinition() {
        Collection messagingServers = this.getInstantMessagingServerManager().getAllInstantMessagingServers();
        return messagingServers.isEmpty() ? null : new XMPPMucConferenceInstantMessagingServerDefinition((InstantMessagingServerDefinition)messagingServers.iterator().next());
    }

    public InstantMessagingServerManager getInstantMessagingServerManager() {
        if (this.instantMessagingServerManager == null) {
            this.instantMessagingServerManager = (InstantMessagingServerManager) ContainerManager.getComponent("instantMessagingServerManager");
        }
        return this.instantMessagingServerManager;
    }

    /**
     *
     * @param server
     * @return XMPPConnection Object
     * @throws XMPPException
     * Attempts to return a valid XMPPConnection Object, will create a new connection if one does not already exist.
     */
    private XMPPTCPConnection getConnection(XMPPMucConferenceInstantMessagingServerDefinition server) throws XMPPException {

        //Return existing connection if defined/connected
        if (this.connection != null ) {
            if (this.connection.isConnected()){
                log.info("Using existing XMPP Connection");
                return this.connection;
            }
        } else {
            log.info("Connection is null");
        }
        log.info("Creating new XMPP Connection");

        //Pull vars from XMPPMucConferanceIMSDef
        String host = server.getHost();
        String serviceName = "talk.google.com".equals(host) ? "gmail.com" : host;
        Integer port = server.getPort();
        String username = server.getUsername();
        String password = server.getPassword();
        log.info("Connecting to " + host + " at " + port + " With user: " + username);

        //Connection Builder
        XMPPTCPConnectionConfiguration.Builder conf = XMPPTCPConnectionConfiguration.builder();
        conf.setServiceName(serviceName);
        conf.setHost(host);
        conf.setPort(port);
        conf.setUsernameAndPassword(username, password);
        conf.setCompressionEnabled(true);
        conf.setConnectTimeout(60);

        //Accept all certs regardless
        try {
            TLSUtils.acceptAllCertificates(conf);
        } catch (NoSuchAlgorithmException e){
            log.trace(e);
        } catch (KeyManagementException e){
            log.trace(e);
        }

        //Require secure connection
        if (server.isSecureConnectionRequired()){
            if (port == null) {
                port = server.isEnforceLegacySsl() ? DEFAULT_SSL_PORT : DEFAULT_TLS_PORT;
            }
            conf.setPort(port);
            conf.setSecurityMode(SecurityMode.required);
        } else {
            conf.setSecurityMode(SecurityMode.disabled);
        }
        conf.setHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        });

        //connect
        new Java7SmackInitializer().initialize();
        this.connection = new XMPPTCPConnection(conf.build());
        try {
            this.connection.connect();
        } catch (SmackException e) {
            log.info("XMPP MUC Connection Error");
            log.info(e.getStackTrace());
        } catch (IOException e) {
            log.info("XMPP MUC Connection Error");
            log.info(e.getStackTrace());
        }

        //login
        try {
            this.connection.login();
        } catch (SmackException e) {
            log.info("XMPP MUC Authentication Error");
            log.info(e.getStackTrace());
        } catch (IOException e) {
            log.info("XMPP MUC Connection Error");
            log.info(e.getStackTrace());
        }

        return this.connection;
    }

}
