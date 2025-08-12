import java.sql.*;
import java.util.*;

public class GripUpdater {
    private static final String DB_PATH = "C:/Program Files/DB Browser for SQLite/kursovaya.db"; // Путь к твоей базе данных
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    private static final String[] WHEELS = {
            "front_left_grip", "front_right_grip", "rear_left_grip", "rear_right_grip"
    };

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                System.out.println("✅ Успешно подключено к базе данных.");

                int lastId = getLastId(conn);
                System.out.println("📌 Начинаем работу с последним ID: " + lastId);

                while (true) {
                    Map<String, Double> currentValues = getCurrentGripValues(conn, lastId);

                    insertNewGripValues(conn, currentValues, lastId);

                    lastId++;
                    System.out.println("➡️ Новый последний ID: " + lastId);
                    Thread.sleep(10_000); // 10 секунд пауза
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static int getLastId(Connection conn) throws SQLException {
        String sql = "SELECT MAX(id) AS max_id FROM vehicles";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("max_id");
            }
        }
        return 0;
    }

    private static Map<String, Double> getCurrentGripValues(Connection conn, int lastId) throws SQLException {
        Map<String, Double> gripValues = new HashMap<>();

        String sql = "SELECT front_left_grip, front_right_grip, rear_left_grip, rear_right_grip FROM vehicles WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, lastId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                gripValues.put("front_left_grip", rs.getDouble("front_left_grip"));
                gripValues.put("front_right_grip", rs.getDouble("front_right_grip"));
                gripValues.put("rear_left_grip", rs.getDouble("rear_left_grip"));
                gripValues.put("rear_right_grip", rs.getDouble("rear_right_grip"));
            }
        }
        return gripValues;
    }

    private static void insertNewGripValues(Connection conn, Map<String, Double> currentValues, int lastId) throws SQLException {
        Random rand = new Random();

        int numWheelsToChange = rand.nextInt(4) + 1;

        List<String> shuffled = new ArrayList<>(Arrays.asList(WHEELS));
        Collections.shuffle(shuffled);
        List<String> wheelsToUpdate = shuffled.subList(0, numWheelsToChange);

        StringBuilder sql = new StringBuilder("INSERT INTO vehicles (name, throttle_position, brake_position, clutch_temperature, gearbox_temperature, brake_temperature, front_left_grip, front_right_grip, rear_left_grip, rear_right_grip) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        double newFrontLeftGrip = currentValues.get("front_left_grip");
        double newFrontRightGrip = currentValues.get("front_right_grip");
        double newRearLeftGrip = currentValues.get("rear_left_grip");
        double newRearRightGrip = currentValues.get("rear_right_grip");

        for (String wheel : wheelsToUpdate) {
            int reduction = rand.nextInt(14) + 7;
            if (wheel.equals("front_left_grip")) newFrontLeftGrip -= reduction;
            if (wheel.equals("front_right_grip")) newFrontRightGrip -= reduction;
            if (wheel.equals("rear_left_grip")) newRearLeftGrip -= reduction;
            if (wheel.equals("rear_right_grip")) newRearRightGrip -= reduction;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setString(1, "Chevrolet Camaro ZL1");
            pstmt.setDouble(2, 100.0);
            pstmt.setDouble(3, 0.0);
            pstmt.setDouble(4, 125.21);
            pstmt.setDouble(5, 93.45);
            pstmt.setDouble(6, 50.0);
            pstmt.setDouble(7, newFrontLeftGrip);
            pstmt.setDouble(8, newFrontRightGrip);
            pstmt.setDouble(9, newRearLeftGrip);
            pstmt.setDouble(10, newRearRightGrip);

            pstmt.executeUpdate();
            System.out.println("📥 Выполнено: Вставлена новая строка с изменениями.");
        }
    }
}
