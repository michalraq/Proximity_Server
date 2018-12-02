package com.michalraq.proximitylightapp.server.Database;

import com.michalraq.proximitylightapp.server.Exceptions.LackOfDatabaseData;
import com.michalraq.proximitylightapp.server.MessageContent;
import com.michalraq.proximitylightapp.server.Util.FileReader;
import com.michalraq.proximitylightapp.server.Util.StringOperations;
import lombok.Getter;


import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class DatabaseManager {

    @Getter private String url ;
    @Getter private Connection connection;

   public DatabaseManager() throws LackOfDatabaseData {
       ArrayList<String> list = FileReader.readDbFile("database.txt");
       String hostName;
       String dbName;
       String user;
       String password;
       if (!list.isEmpty()) {
           hostName = list.get(0);
           dbName = list.get(1);
           user = list.get(2);
           password = list.get(3);
           url = String.format("jdbc:sqlserver://%s:1433;database=%s;user=%s;password=%s;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;", hostName, dbName, user, password);


       } else {
           hostName = "";
           dbName = "";
           user = "";
           password = "";
           throw new LackOfDatabaseData();
       }
   }

    public Boolean connectDatabase(){
        try {
                connection = DriverManager.getConnection(url);
                String schema = connection.getSchema();
                System.out.println("Successful connection - Schema: " + schema);
                System.out.println("Connection with database established");
                return true;
        }
        catch (Exception e) {
            System.err.println("Error occured during connect to database!");
            return false;
        }
    }

    public Boolean disconnectDatabase(){
        try {
            if(connection!=null)
            connection.close();
            System.out.println("Connection with database closed");
            return true;
        } catch (SQLException e) {
            System.err.println("Error occured during disconnect to database!");
        return false;
        }
    }

    public void insertIntoCmtStatusTIME_IN(MessageContent message) {

        String dateNow = StringOperations.getCurrentDateYMDHmS();

        String sql = "INSERT DEV.CMT_STATUS VALUES \n" +
                "(NEXT VALUE FOR DEV.SEQ_ID_STATUS,CAST('" + dateNow + "'AS datetime),null,'"+message.getPlace()+"')";

        try (Statement stm = connection.createStatement()) {
            stm.execute(sql);
        } catch (SQLException e) {
            System.err.println("Blad INSERT_IN");
            e.printStackTrace();
        }
    }
    @SuppressWarnings("Duplicates")
    public void insertIntoCmtStatusTIME_OUT(MessageContent message){

        String date = StringOperations.getCurrentDateYMDHmS();
        date=StringOperations.addSingleQuotes(date);
        String place = StringOperations.addSingleQuotes(message.getPlace());

        boolean flag=false;

        String SQL = "select CMT_STATUS.ID_STATUS from CMT_STATUS \n" +
                "where ID_STATUS = (select current_value from sys.sequences where name = 'SEQ_ID_STATUS');";

           //sprawdzenie czy istnieje rekord z aktualna wartoscia sekwencji, jezeli nie to nic nie rob
           try (Statement stm = connection.createStatement()) {
           ResultSet rs = stm.executeQuery(SQL);
           rs.next();
           flag=rs.getBoolean("ID_STATUS");

               if (!rs.wasNull()) {
                   flag = true;
               }else {
                   flag=false;
               }
           } catch (SQLException e) {
               System.err.println("Blad podczas sprawdzenia czy istnieje rekord");
               e.printStackTrace();
           }

        if(flag) {

         String sql = "update DEV.CMT_STATUS\n" +
                 "set TIME_OUT = CAST("+date+" as datetime)\n" +
                 "where ID_STATUS= (select current_value from sys.sequences where name = 'SEQ_ID_STATUS')\n" +
                 "AND CMT_PLACES_PLACE="+place;
            try {
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.executeUpdate();
                ps.close();

            } catch (SQLException e) {
                System.err.println("Blad insertIntoCmtStatusTIME_OUT");
            }
        }else
        {
            System.err.println("Brak rekordu do Zaktualizowania");
        }
   }

    @SuppressWarnings("Duplicates")
    public void updateStatusOfLight(MessageContent messageContent){
       //true - on
       //false - off
        String sql;
        String place = StringOperations.addSingleQuotes(messageContent.getPlace());
        if (messageContent.getSignal()==1) {

             sql = "update DEV.CMT_PLACES\n" +
                    "set POWER_ON = 1,MODIFICATION_DATE = getdate() + '01:0'\n" +
                     "where PLACE = "+place ;
        } else {
             sql = "update DEV.CMT_PLACES\n" +
                    "set POWER_ON = 0,MODIFICATION_DATE = getdate() + '01:0'\n" +
                    "where PLACE = "+place ;
        }

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            System.err.println("Blad podczas zmiany statusu swiatla");
        }

    }

    //TODO DOROBIC AKTUALIZACJE STATUSU W TABELI PLACE
    //TODO Jezeli insert , a poprzedni rekord ma wartosc null przy wyjsciu to wyslij sygnal z wylaczeniem swiatla

}