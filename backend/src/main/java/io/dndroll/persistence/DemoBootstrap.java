package io.dndroll.persistence;

import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final Environment env;
    private final PasswordEncoder passwords;
    public DemoBootstrap(JdbcTemplate jdbc, Environment env, PasswordEncoder passwords) { this.jdbc = jdbc; this.env = env; this.passwords = passwords; }
    private String required(String key) {
        String value = env.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("The demo profile requires " + key);
        return value;
    }
    @Transactional
    public void run(ApplicationArguments args) {
        String dm = env.getProperty("demo.dm.username", "dm");
        String player = env.getProperty("demo.player.username", "player");
        String dmPassword = required("demo.dm.password"); String playerPassword = required("demo.player.password");
        if (dm.isBlank() || player.isBlank() || dm.equals(player)) throw new IllegalStateException("Demo accounts must have distinct nonempty usernames");
        jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?) on conflict (username) do nothing", UUID.randomUUID(), dm, passwords.encode(dmPassword));
        jdbc.update("insert into app_user(id,username,password_hash) values (?,?,?) on conflict (username) do nothing", UUID.randomUUID(), player, passwords.encode(playerPassword));
        UUID campaign = UUID.fromString("84b37902-114a-4a26-9f66-4041022a2024");
        UUID scene = UUID.fromString("4f8875af-2b63-4a24-9cae-19891aa83d45");
        UUID hero = UUID.fromString("58260481-7e3b-41b4-aa77-0d57a4f57382");
        UUID enemy = UUID.fromString("7b66348b-5c1f-4f02-a635-9e28bdeeb5cf");
        int created = jdbc.update("insert into campaign(id,name) values (?,?) on conflict do nothing", campaign, "First campaign");
        // Seed permissions only when this transaction creates the demo campaign.
        // A later restart must preserve revoked memberships and control grants.
        if (created == 0) return;
        jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,?) on conflict do nothing", campaign, dm, "DM");
        jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,?) on conflict do nothing", campaign, player, "PLAYER");
        jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,?,24,16,48) on conflict do nothing", scene, campaign, "First scene");
        jdbc.update("update campaign set current_scene_id=? where id=?", scene, campaign);
        jdbc.update("insert into token(id,scene_id,name,x,y,hidden) values (?,?,?,2,3,false) on conflict do nothing", hero, scene, "Adventurer");
        jdbc.update("insert into token(id,scene_id,name,x,y,hidden) values (?,?,?,12,8,true) on conflict do nothing", enemy, scene, "Hidden enemy");
        jdbc.update("insert into token_controller(token_id,username) values (?,?) on conflict do nothing", hero, player);
    }
}
