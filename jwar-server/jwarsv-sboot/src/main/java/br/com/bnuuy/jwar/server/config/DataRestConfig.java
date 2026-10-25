package br.com.bnuuy.jwar.server.config;

import br.com.bnuuy.jwar.server.domain.Match;
import br.com.bnuuy.jwar.server.domain.Room;
import br.com.bnuuy.jwar.server.domain.RoomMember;
import br.com.bnuuy.jwar.server.domain.RoomMessage;
import br.com.bnuuy.jwar.server.domain.User;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * Hides our JPA repositories from Spring Data REST auto-exposure — we ship our own
 * controllers and don't want HAL endpoints under /. (spring-boot-starter-data-rest is
 * a transitive dep used by Spring Modulith.)
 */
@Configuration
public class DataRestConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        config.disableDefaultExposure();
        config.setBasePath("/internal-data-rest-disabled");
        config.exposeIdsFor(User.class, Room.class, RoomMember.class, RoomMessage.class, Match.class);
    }
}
