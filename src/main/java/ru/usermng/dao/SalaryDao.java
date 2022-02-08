package ru.usermng.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.usermng.model.Salary;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class SalaryDao {
    private static final Logger logger = LoggerFactory.getLogger(SalaryDao.class);

    public Salary create(Connection connection, int value) {
        try {
            try (var pst = connection.prepareStatement("insert into salary(val) values (?)", Statement.RETURN_GENERATED_KEYS)) {
                pst.setInt(1, value);
                pst.executeUpdate();
                try (var keys = pst.getGeneratedKeys()) {
                    keys.next();
                    var salary = new Salary(keys.getLong(1), value);
                    return salary;
                }
            }
        } catch (SQLException ex) {
            throw new SellaryOperationException("salary creation error", ex);
        }
    }

    public void update(Connection connection, long id, int val) {
        try {
            try (var pst = connection.prepareStatement("update salary set val = val + ? where id = ?")) {
                pst.setInt(1, val);
                pst.setLong(2, id);

                pst.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new SellaryOperationException("salary creation error", ex);
        }
    }

    public Optional<Salary> select(Connection connection, long id) {
        try {
            try (var pst = connection.prepareStatement("select val from salary where id  = ?")) {
                pst.setLong(1, id);
                try (var rs = pst.executeQuery()) {
                    if (rs.next()) {
                        var user = new Salary(id, rs.getInt("val"));
                        return Optional.of(user);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new SellaryOperationException("salary selection error", ex);
        }
        return Optional.empty();
    }
}
