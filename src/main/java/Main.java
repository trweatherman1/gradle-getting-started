import ratpack.server.RatpackServer;
import ratpack.groovy.template.TextTemplateModule;
import ratpack.guice.Guice;
import ratpack.server.RatpackServer;
import ratpack.server.BaseDir;
import static ratpack.groovy.Groovy.groovyTemplate;
import static ratpack.groovy.Groovy.ratpack;

import static javax.measure.unit.SI.KILOGRAM;
import javax.measure.quantity.Mass;
import org.jscience.physics.model.RelativisticModel;
import org.jscience.physics.amount.Amount;

import java.util.*;
import java.sql.*;
import com.heroku.sdk.jdbc.DatabaseUrl;

public class Main {
  public static void main(String... args) throws Exception {
    RatpackServer.start(b -> b
        .serverConfig(s -> s
          .baseDir(BaseDir.find())
          .env()
        )
        .registry(
          Guice.registry(s -> s
              .module(TextTemplateModule.class, conf ->
                  conf.setStaticallyCompile(true)
              )
          )
        )
        .handlers(c -> {
          c
            .get("index.html", ctx -> {
              ctx.redirect(301, "/");
            })
            .get(ctx -> ctx.render(groovyTemplate("index.html")))
            .get("hello", ctx -> {
            RelativisticModel.select();
            Amount<Mass> m = Amount.valueOf("12 GeV").to(KILOGRAM);
            ctx.render("E=mc^2: 12 GeV = " + m.toString());
            })
            .get("db", ctx -> {
              Connection connection = null;
              Map<String, Object> attributes = new HashMap<>();
              try {
                boolean local = !"cedar-14".equals(System.getenv("STACK"));
                connection = DatabaseUrl.extract(local).getConnection();

                Statement stmt = connection.createStatement();
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
                stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
                ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

                ArrayList<String> output = new ArrayList<String>();
                while (rs.next()) {
                  output.add( "Read from DB: " + rs.getTimestamp("tick"));
                }

                attributes.put("results", output);
                ctx.render(groovyTemplate(attributes, "db.html"));
              } catch (Exception e) {
                attributes.put("message", "There was an error: " + e);
                ctx.render(groovyTemplate(attributes, "error.html"));
              } finally {
                if (connection != null) try{connection.close();} catch(SQLException e){}
              }
            })
            .files(f -> f.dir("public"));
        }
      )
    );
  }
}
