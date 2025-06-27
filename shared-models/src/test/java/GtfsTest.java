import com.bustracker.shared.gtfs.GtfsRealtime;
import org.junit.jupiter.api.Test;

public class GtfsTest {

  @Test
  public void testGtfsProtobufGeneration() {
    // Just verify the classes are generated and accessible
    GtfsRealtime.FeedMessage.Builder builder = GtfsRealtime.FeedMessage.newBuilder();

    // Create a simple header to verify everything works
    GtfsRealtime.FeedHeader header = GtfsRealtime.FeedHeader.newBuilder()
        .setGtfsRealtimeVersion("2.0")
        .setTimestamp(System.currentTimeMillis() / 1000)
        .build();

    GtfsRealtime.FeedMessage feedMessage = builder
        .setHeader(header)
        .build();

    System.out.println("GTFS Protobuf classes generated successfully!");
    System.out.println("Version: " + feedMessage.getHeader().getGtfsRealtimeVersion());
  }
}
