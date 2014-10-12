package ebay.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Repository;

import ebay.data.Employee;
import ebay.data.EventData;
import ebay.data.FilterResponseData;
import ebay.data.LoginBean;
import ebay.data.Participant;
import ebay.data.UserSignUp;
import ebay.exception.UserDataException;
import ebay.utilities.ApplicationUtility;
import ebay.utilities.DatabaseConnection;

@Repository
public class EmployeeDataDaoImpl implements EmployeeDataDao {

	@Override
	public Employee getEmployee() throws IOException, SQLException {
		// TODO Auto-generated method stub

		Employee emp = new Employee();
		Connection dbCon = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			dbCon = DatabaseConnection.getConnection();
			stmt = dbCon.prepareStatement(ApplicationUtility
					.getPropertyValue("getEmployee"));
			rs = stmt.executeQuery(ApplicationUtility
					.getPropertyValue("getEmployee"));

			while (rs.next()) {
				emp.setFirstName(rs.getString(4));
				emp.setEmail(rs.getString(3));
			}

		} catch (SQLException ex) {
			ex.printStackTrace();

		} finally {
			DatabaseConnection.closeConnection(dbCon);
		}

		return emp;
	}

	public String insertHistoricData(Map<String, Participant> pMap,
			Set<String> locationSet, String eventName, int year)
			throws IOException, SQLException {

		String result = "fail";
		Connection dbCon = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			int eventId = 1;

			dbCon = DatabaseConnection.getConnection();
			dbCon.setAutoCommit(false);
			String sql = "select max(event_id) from event";
			stmt = dbCon.prepareStatement(sql);
			rs = stmt.executeQuery();
			try {
				rs.next();
				eventId = ((int) rs.getObject(1)) + 1;
			} catch (Exception ex) {
				// ex.printStackTrace();
			}
			sql = "insert into event (event_name,event_id,event_year) values (?,?,?)";
			stmt = dbCon.prepareStatement(sql);
			int colIndex = 1;
			stmt.setString(colIndex++, eventName);
			stmt.setInt(colIndex++, eventId++);
			stmt.setInt(colIndex++, year);
			stmt.executeUpdate();
			sql = "insert into location (event_id,event_location) values (?,?)";
			stmt = dbCon.prepareStatement(sql);
			for (String loc : locationSet) {
				colIndex = 1;
				stmt.setInt(colIndex++, eventId);
				stmt.setString(colIndex, loc);
				stmt.addBatch();
			}
			stmt.executeBatch();
			sql = "select DISTINCT(emp_id) from employee";
			stmt = dbCon.prepareStatement(sql);
			rs = stmt.executeQuery();
			
			Collection<Participant> pMapValues = pMap.values();
			
			while (rs.next()) {
				String key = rs.getString(1);
				System.out.println("key: " + key);
				if (pMap.keySet().contains(key)) {
					System.out.println("removing");
					pMap.remove(key);
				}
			}
			if (pMap.size() > 0) {
				sql = "insert into employee(emp_id,emp_fname,emp_lname,emp_email) values (?,?,?,?)";
				stmt = dbCon.prepareStatement(sql);
				for (Participant p : pMap.values()) {
					colIndex = 1;
					stmt.setString(colIndex++, p.getEmpId());
					stmt.setString(colIndex++, p.getFname());
					stmt.setString(colIndex++, p.getLname());
					stmt.setString(colIndex++, p.getEmailId());
					stmt.addBatch();
				}
				stmt.executeBatch();

			}
			
			sql = "insert into participant(emp_id,event_id) values (?,?)";
			stmt = dbCon.prepareStatement(sql);
			for (Participant p : pMapValues) {
				colIndex = 1;
				stmt.setInt(colIndex++, Integer.parseInt(p.getEmpId()));
				stmt.setInt(colIndex++, eventId);
				stmt.addBatch();
			}
			stmt.executeBatch();

			dbCon.commit();
			result = "success";
		} catch (Exception ex) {
			ex.printStackTrace();

		} finally {
			// close connection ,stmt and resultset here
			/*
			 * rs.close(); stmt.close();
			 */

			DatabaseConnection.closeConnection(dbCon);

		}
		return result;
	}

	@Override
	public void createUser(UserSignUp userData) throws SQLException,
			UserDataException {
		// TODO Auto-generated method stub
//		String result = "fail";
		Connection dbCon = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		if (!checkExistingUser(userData)) {
			try {
				System.out.println("User not exists..");
				dbCon = DatabaseConnection.getConnection();
				dbCon.setAutoCommit(false);
				String sql = "insert into user (emp_id,password,level) values(?,?,?)";
				stmt = dbCon.prepareStatement(sql);
				stmt.setInt(1, userData.getEmpId());
				stmt.setString(2, userData.getPassword());
				stmt.setInt(
						3,
						Integer.parseInt(ApplicationUtility.getPropertyValue(
								"general").toString()));
				stmt.executeUpdate();
				System.out.println("Data entered into user table");
				sql = "select count(emp_id) from employee where emp_id=?";
				stmt = dbCon.prepareStatement(sql);
				stmt.setInt(1, userData.getEmpId());
				rs = stmt.executeQuery();
				rs.next();
				if (rs.getInt(1) == 0) {

					sql = "insert into employee (emp_id,emp_email,emp_fname,emp_lname) values(?,?,?,?)";
					stmt = dbCon.prepareStatement(sql);
					stmt.setInt(1, userData.getEmpId());
					stmt.setString(2, userData.getEmail());
					stmt.setString(3, userData.getFirstName());
					stmt.setString(4, userData.getLastName());
					stmt.executeUpdate();
					System.out.println("Data entered into employee table");
				}
				dbCon.commit();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				DatabaseConnection.closeConnection(dbCon);
			}
		} else
			throw new UserDataException("User already exists!!");
	}

	private boolean checkExistingUser(UserSignUp userData) {

		Connection dbCon = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			dbCon = DatabaseConnection.getConnection();
			dbCon.setAutoCommit(false);
			String sql = "select count(emp_id) from user where emp_id=?";
			stmt = dbCon.prepareStatement(sql);
			stmt.setInt(1, userData.getEmpId());
			rs = stmt.executeQuery();
			try {
				System.out.println("In check existing user functionality");
				rs.next();
				if (rs.getInt(1) == 0)
					return false;
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {

				DatabaseConnection.closeConnection(dbCon);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public boolean authorizeUser(LoginBean loginBean) {
		// TODO Auto-generated method stub
		Connection dbCon = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			dbCon = DatabaseConnection.getConnection();
			dbCon.setAutoCommit(false);
			String sql = "select * from user where emp_id=?";
			stmt = dbCon.prepareStatement(sql);
			stmt.setInt(1, loginBean.getEmpId());
			rs = stmt.executeQuery();
			try {

				rs.next();
				if (rs.getString(2) == null)
					return false;
				else {

					System.out.println("from resultset-" + rs.getString(2)
							+ "and from object--" + loginBean.getPassword());
					String password = rs.getString(2);
					if (password.equals(loginBean.getPassword()))
						return true;
					else
						return false;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				return false;
			} finally {

				DatabaseConnection.closeConnection(dbCon);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;

	}

	@Override
	public List<String> getAllEvents() {
		List<String> lstEvent = new ArrayList<String>();
		Connection dbCon = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			dbCon = DatabaseConnection.getConnection();
			String sql = "SELECT * FROM event GROUP BY event_name ORDER BY event_name DESC";
			stmt = dbCon.prepareStatement(sql);
			rs = stmt.executeQuery();
			try {

				while (rs.next())
					lstEvent.add(rs.getString(2));

				rs.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {

				DatabaseConnection.closeConnection(dbCon);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return lstEvent;
	}

	@Override
	public List<String> getAllLocations() {
		// TODO Auto-generated method stub
		List<String> lstLocation = new ArrayList<String>();
		Connection dbCon = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			dbCon = DatabaseConnection.getConnection();
			String sql = "SELECT * FROM location GROUP BY event_location ORDER BY event_location DESC";
			stmt = dbCon.prepareStatement(sql);
			rs = stmt.executeQuery();
			try {

				while (rs.next())
					lstLocation.add(rs.getString(2));

				rs.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {

				DatabaseConnection.closeConnection(dbCon);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return lstLocation;
	}

	@Override
	public List<String> getAllYears() {
		// TODO Auto-generated method stub
		List<String> lstYear = new ArrayList<String>();
		Connection dbCon = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			dbCon = DatabaseConnection.getConnection();
			String sql = "SELECT * FROM event GROUP BY event_year ORDER BY event_year DESC";
			stmt = dbCon.prepareStatement(sql);
			rs = stmt.executeQuery();
			try {

				while (rs.next())
					lstYear.add(rs.getString(3));

				rs.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {

				DatabaseConnection.closeConnection(dbCon);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lstYear;
	}

	@Override
	public List<FilterResponseData> getParticipationListByCondition(
			String filter, String filterKey) {

		// TODO Auto-generated method stub
		List<FilterResponseData> list = new ArrayList<FilterResponseData>();
		Connection dbCon = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {

			dbCon = DatabaseConnection.getConnection();
			String sql = "select e.event_id," + "	e.event_name,  emp.emp_id,"
					+ " emp.emp_fname, emp.emp_lname,"
					+ " emp.emp_email, loc.event_location," + " e.event_year "
					+ " from event e "
					+ " inner join participant p on (e.event_id = p.event_id)"
					+ " inner join employee emp on (emp.emp_id = p.emp_id)"
					+ " inner join location loc on (loc.event_id = e.event_id)"
					+ " @@condition@@";

			if (filterKey.equalsIgnoreCase("all")) {
				sql = sql.replace("@@condition@@", "");
			} else {
				if (filter.equals("year")) {
					sql = sql.replace("@@condition@@","where event_year="+filterKey);
				} else if (filter.equals("event")) {
					sql = sql.replace("@@condition@@","where event_name='"+filterKey+"'");
				} else if (filter.equals("location")) {
					sql = sql.replace("@@condition@@","where event_location='"+filterKey+"'");
				}
			}
			stmt = dbCon.prepareStatement(sql);
			rs = stmt.executeQuery();
			try {

				while (rs.next()) {
					FilterResponseData data = new FilterResponseData();

					EventData event = new EventData();
					event.setEventName(rs.getString("event_name"));
					event.setEventId(rs.getString("event_id"));
					event.setEventYear("event_year");
					event.setLocation(rs.getString("event_location"));

					data.setEventData(event);

					Participant part = new Participant();
					part.setEmailId(rs.getString("emp_email"));
					
					part.setFname(rs.getString("emp_fname"));
					part.setLname(rs.getString("emp_lname"));
					part.setEmpId(rs.getString("emp_id"));

					data.setParticipant(part);

					list.add(data);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				DatabaseConnection.closeConnection(dbCon);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public String insertYearData(Map<String, Participant> pMap,
			Map<Integer, List<String>> eventUserMap,
			Map<String, List<Integer>> eventLocationMap,
			Map<String, Integer> eventMap, String year) throws SQLException {
		// 

		String result = "fail";
		Connection dbCon = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		Map<Integer,Integer> indextoEventIdMap = new HashMap<Integer,Integer>();
		
		try {

			int eventId = 1;

			dbCon = DatabaseConnection.getConnection();
			dbCon.setAutoCommit(false);
			
			String sql = "select max(event_id) from event";
			stmt = dbCon.prepareStatement(sql);
			rs = stmt.executeQuery();
			try {
				rs.next();
				eventId = ((int) rs.getObject(1)) + 1;
			} catch (Exception ex) {
				// ex.printStackTrace();
			}
			sql = "insert into event (event_name,event_id,event_year) values (?,?,?)";
			stmt = dbCon.prepareStatement(sql);
			int colIndex = 1;
			for(String eventName : eventMap.keySet()){
				
				stmt.setString(colIndex++, eventName);
				stmt.setInt(colIndex++, eventId);
				stmt.setInt(colIndex++, Integer.parseInt(year));
				stmt.addBatch();
				
				indextoEventIdMap.put(eventMap.get(eventName), Integer.valueOf(eventId));
				eventId++;
				
			}
			stmt.executeUpdate();
			sql = "insert into location (event_id,event_location) values (?,?)";
			stmt = dbCon.prepareStatement(sql);
			for (String loc : eventLocationMap.keySet()) {
				for(Integer i : eventLocationMap.get(loc)){
					colIndex = 1;
					stmt.setInt(colIndex++, indextoEventIdMap.get(i));
					stmt.setString(colIndex, loc);
					stmt.addBatch();
				}
			}
			stmt.executeBatch();

			sql = "select DISTINCT(emp_id) from employee";
			stmt = dbCon.prepareStatement(sql);
			rs = stmt.executeQuery();
			Collection<Participant> pMapValues = pMap.values();
			while (rs.next()) {
				String key = rs.getString(1);
				System.out.println("key: " + key);
				if (pMap.keySet().contains(key)) {
					System.out.println("removing");
					pMap.remove(key);
				}
			}
			if (pMap.size() > 0) {
				sql = "insert into employee(emp_id,emp_fname,emp_lname,emp_email) values (?,?,?,?)";
				stmt = dbCon.prepareStatement(sql);
				for (Participant p : pMap.values()) {
					colIndex = 1;
					stmt.setString(colIndex++, p.getEmpId());
					stmt.setString(colIndex++, p.getFname());
					stmt.setString(colIndex++, p.getLname());
					stmt.setString(colIndex++, p.getEmailId());
					stmt.addBatch();
				}
				stmt.executeBatch();

			}
			
			sql = "insert into participant(emp_id,event_id) values (?,?)";
			stmt = dbCon.prepareStatement(sql);
			for (Integer index : eventUserMap.keySet()) {
				for(String empId :eventUserMap.get(index)) {
					colIndex = 1;
					stmt.setInt(colIndex++, Integer.parseInt(empId));
					stmt.setInt(colIndex++, indextoEventIdMap.get(index));
					stmt.addBatch();
				}
			}
			stmt.executeBatch();

			dbCon.commit();
			result = "success";
		} catch (Exception ex) {
			ex.printStackTrace();

		} finally {
			// close connection ,stmt and resultset here
			/*
			 * rs.close(); stmt.close();
			 */

			DatabaseConnection.closeConnection(dbCon);

		}
		return result;
	}

}
