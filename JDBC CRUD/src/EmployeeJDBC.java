import java.sql.*;
import java.util.Scanner;

public class EmployeeJDBC {

    // Database URL, Username, Password
    static final String DB_URL = "jdbc:mysql://localhost:3306/company";
    static final String USER = "root";
    static final String PASS = "root";

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        try {

            // Load JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish Connection
            Connection con = DriverManager.getConnection(DB_URL, USER, PASS);

            // Insert Employee Details
            System.out.println("Enter Employee ID:");
            int id = sc.nextInt();
            sc.nextLine();

            System.out.println("Enter Employee Name:");
            String name = sc.nextLine();

            System.out.println("Enter Employee Salary:");
            double salary = sc.nextDouble();

            // SQL Insert Query
            String insertQuery =
                    "INSERT INTO employee(emp_id, emp_name, emp_salary) VALUES(?,?,?)";

            PreparedStatement pst = con.prepareStatement(insertQuery);

            pst.setInt(1, id);
            pst.setString(2, name);
            pst.setDouble(3, salary);

            int rows = pst.executeUpdate();

            if (rows > 0) {
                System.out.println("Employee record inserted successfully!");
            }

            // Display Employee Records
            String selectQuery = "SELECT * FROM employee";

            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery(selectQuery);

            System.out.println("\nEmployee Records:");
            System.out.println("-------------------------------");

            while (rs.next()) {
                System.out.println(
                        rs.getInt("emp_id") + " | " +
                                rs.getString("emp_name") + " | " +
                                rs.getDouble("emp_salary")
                );
            }

            // Close Connections
            rs.close();
            st.close();
            pst.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}