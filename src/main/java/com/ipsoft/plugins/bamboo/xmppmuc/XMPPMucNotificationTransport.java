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
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;

import java.util.Collection;

public class XMPPMucNotificationTransport implements NotificationTransport
{
    private static final Logger log = Logger.getLogger(XMPPMucNotificationTransport.class);

    private static final Integer DEFAULT_TLS_PORT = 5222;
    private static final Integer DEFAULT_SSL_PORT = 5223;
    private static final String DEFAULT_RESOURCE = "Bamboo";
    private InstantMessagingServerManager instantMessagingServerManager;
    private XMPPConnection connection;
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
            try {
                MultiUserChat muc = new MultiUserChat(this.getConnection(server), room);
                this.muc = muc;
            } catch (XMPPException e) {
                log.error("Error creating XMPP MUC Instance");
                log.trace(e.getStackTrace());
            }


            //Add 4 letters to make the name unique (Anti-troll)
            String code = RandomStringUtils.randomAlphanumeric(4).toUpperCase();
            String DEFAULT_CHANNEL_USERNAME = "Bamboo Test Agent " + code;

            try {
                if (roompw != null && !roompw.isEmpty()) {
                    this.muc.join(DEFAULT_CHANNEL_USERNAME, this.roompw);
                } else {
                    this.muc.join(DEFAULT_CHANNEL_USERNAME);
                }
            } catch (XMPPException e) {
                log.error("Error joining MUC room: " + this.room);
                log.trace(e.getStackTrace());
            }
            try {
                this.muc.sendMessage(message);
            } catch (XMPPException e) {
                log.error("Error sending message to MUC");
                log.trace(e.getStackTrace());
            }
            // Leave the channel when done
            this.muc.leave();
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
    private XMPPConnection getConnection(XMPPMucConferenceInstantMessagingServerDefinition server) throws XMPPException {
        ConnectionConfiguration configuration;
        if (this.connection != null) {
            if (this.connection.isConnected()) {
                if (server.equals(this.imserver)){
                    log.info("Using existing XMPP Connection");
                    return this.connection;
                } else {
                    log.info("Not a object or something");
                }
            } else {
                log.info("Not Connected");
            }
        } else {
            log.info("Connection is null");
        }
        //if (this.connection != null && this.connection.isConnected() && server.equals((Object)this.imserver)) {
        //    log.info("Using existing XMPP Connection");
        //    return this.connection;
        //}
        log.info("Creating new XMPP Connection");

        String host = server.getHost();
        String serviceName = "talk.google.com".equals(host) ? "gmail.com" : host;
        Integer port = server.getPort();
        String username = server.getUsername();
        String password = server.getPassword();

        log.info("Connecting to " + host + " at " + port + " With user: " + username);
        ConnectionConfiguration.SecurityMode securityMode = ConnectionConfiguration.SecurityMode.enabled;
        if (server.isSecureConnectionRequired()) {
            if (port == null) {
                port = server.isEnforceLegacySsl() ? DEFAULT_SSL_PORT : DEFAULT_TLS_PORT;
            }
            configuration = new ConnectionConfiguration(host, port.intValue(), serviceName);
            configuration.setReconnectionAllowed(false);
            if (server.isEnforceLegacySsl()) {
                //configuration.setSocketFactory((SocketFactory)new SSLSocketFactoryForBamboo());
            } else {
                securityMode = ConnectionConfiguration.SecurityMode.required;
            }
        } else {
            configuration = port != null ? new ConnectionConfiguration(host, port.intValue(), serviceName) : new ConnectionConfiguration(serviceName);
        }
        configuration.setSecurityMode(securityMode);
        this.imserver = server;
        this.connection = new XMPPConnection(configuration);
        this.connection.connect();
        this.connection.login(server.getUsername(), server.getPassword(), StringUtils.defaultIfEmpty(server.getResource(), "Bamboo"));
        return this.connection;
    }

}
