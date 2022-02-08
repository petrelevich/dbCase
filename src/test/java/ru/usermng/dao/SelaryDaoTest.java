package ru.usermng.dao;


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.usermng.model.Salary;

import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SelaryDaoTest {
    private static final Logger logger = LoggerFactory.getLogger(SalaryDao.class);
    private static final PostgreSQLContainer<PostgreSQLContainerShared> POSTGRE_SQL_CONTAINER = PostgreSQLContainerShared.getInstance();

    static {
        POSTGRE_SQL_CONTAINER.start();
    }

    private final CountDownLatch latch = new CountDownLatch(2);

    @Test
    void test() throws SQLException, InterruptedException {

        //given
        var salaryDao = new SalaryDao();
        var ds = new DataSource(POSTGRE_SQL_CONTAINER.getJdbcUrl(),
                POSTGRE_SQL_CONTAINER.getUsername(),
                POSTGRE_SQL_CONTAINER.getPassword());

        //подготовка данных для теста
        Salary salaryCreated;
        try (var connection = ds.getConnection()) {
            salaryCreated = salaryDao.create(connection, 100);
            connection.commit();
        }
        logger.info("created salary:{}", salaryCreated);

        var addValue = 100;

        //when проверяем просто два последовательных update-а
        update(ds, salaryDao, salaryCreated.getId(), addValue);
        update(ds, salaryDao, salaryCreated.getId(), addValue);

        Salary selectedSalary;
        try (var connection = ds.getConnection()) {
            selectedSalary = salaryDao.select(connection, salaryCreated.getId()).orElseThrow();
        }

        //then
        assertThat(selectedSalary.getVal()).isEqualTo(salaryCreated.getVal() + addValue * 2);

        //when эмуляция двух одновременных update-ов
        var thread1 = new Thread(() -> updateLatch(ds, salaryDao, salaryCreated.getId(), addValue));
        var thread2 = new Thread(() -> updateLatch(ds, salaryDao, salaryCreated.getId(), addValue));

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        Salary selectedSalaryLatch;
        try (var connection = ds.getConnection()) {
            selectedSalaryLatch = salaryDao.select(connection, salaryCreated.getId()).orElseThrow();
        }

        //then проверяем, что произошло задвоение
        assertThat(selectedSalaryLatch.getVal()).isEqualTo(selectedSalary.getVal() + addValue * 2);

    }

    void updateLatch(DataSource ds, SalaryDao salaryDao, long id, int addValue) {
        try (var connection = ds.getConnection()) {
            salaryDao.update(connection, id, addValue);
            logger.info("before commit");
            // убеждаемся, что обе сессии выполнили update и готовы "одновременно" выполнить commit
            latch.countDown();
            latch.await(10, TimeUnit.SECONDS);
            connection.commit();
        } catch (Exception ex) {
            logger.error("err:{}", ex.getMessage(), ex);
        }
    }


    void update(DataSource ds, SalaryDao salaryDao, long id, int addValue) {
        try (var connection = ds.getConnection()) {
            salaryDao.update(connection, id, addValue);
            connection.commit();
        } catch (Exception ex) {
            logger.error("err:{}", ex.getMessage(), ex);
        }
    }
}