package weather.election;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.PutParams;
import com.ecwid.consul.v1.session.model.NewSession;
import com.ecwid.consul.v1.session.model.Session;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;

public class LeaderElection {

    private static final Logger LOG = LoggerFactory.getLogger(LeaderElection.class);

    private final AtomicReference<String> sessionId = new AtomicReference<>();

    private final ObjectMapper mapper;
    private final Contender contender;
    private final ConsulClient consul;
    private final LeaderElectionProperties properties;

    LeaderElection(Contender contender, ConsulClient consul, LeaderElectionProperties properties) {
        this.contender = Objects.requireNonNull(contender);
        this.properties = Objects.requireNonNull(properties);
        this.consul = Objects.requireNonNull(consul);
        this.mapper = new ObjectMapper();
    }

    @Scheduled(initialDelay = 10_000, fixedDelay = 1000)
    public void election() {
        try {
            if (!sessionExists(sessionId.get())) {
                sessionId.set(sessionCreate());
            }

            if (tryLock(sessionId.get())) {
                LOG.info("elected as or still a leader");
            } else {
                LOG.info("failed to win election");
            }
        } catch (Exception ex) {
            LOG.warn("unable to participate in election", ex);
        }
    }

    private boolean tryLock(String sessionId) throws JsonProcessingException {
        PutParams putParams = new PutParams();
        putParams.setAcquireSession(sessionId);
        String contenderJson = mapper.writeValueAsString(contender);

        Response<Boolean> booleanResponse = consul.setKVValue(properties.getKey(), contenderJson, putParams);

        return booleanResponse.getValue();
    }

    private boolean sessionExists(String sessionId) {

        if (sessionId==null) {
            return false;
        }

        Response<Session> sessionInfo = consul.getSessionInfo(sessionId, QueryParams.DEFAULT);

        return sessionInfo.getValue()!=null;
    }

    private String sessionCreate() {

        final NewSession newSession = new NewSession();
        newSession.setName(contender.getServiceId());

        newSession.setChecks(asList("serfHealth", "service:" + contender.getServiceId()));

        return consul.sessionCreate(newSession, QueryParams.DEFAULT).getValue();
    }

    private String toJson(Contender contender) {
        try {
            return mapper.writeValueAsString(contender);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("unable to serialize contender: ", e);
        }
    }
}
