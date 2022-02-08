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

class SalaryDaoTest {
    private static final Logger logger = LoggerFactory.getLogger(SalaryDao.class);
    private static final PostgreSQLContainer<PostgreSQLContainerShared> POSTGRE_SQL_CONTAINER = PostgreSQLContainerShared.getInstance();

    static {
        POSTGRE_SQL_CONTAINER.start();
    }

    private final CountDownLatch latchUpdate = new CountDownLatch(2);
    private final CountDownLatch latchCommit = new CountDownLatch(2);

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
        logger.info("updateLatch");
        try (var connection = ds.getConnection()) {
            // убеждаемся, что обе сессии готовы выполнить update
            latchUpdate.countDown();
            latchUpdate.await(10, TimeUnit.SECONDS);
            logger.info("before update");

            salaryDao.update(connection, id, addValue);

            // убеждаемся, что обе сессии готовы выполнить commit
            latchCommit.countDown();
            var timeOut = latchCommit.await(10, TimeUnit.SECONDS);
            logger.info("before commit, timeOut:{}", timeOut);

            connection.commit();
        } catch (Exception ex) {
            logger.error("err:{}", ex.getMessage(), ex);
        }
    }
/* Смотрим логи
18:33:58.361 [Thread-3] INFO  ru.usermng.dao.SalaryDao - before update
18:33:58.361 [Thread-4] INFO  ru.usermng.dao.SalaryDao - before update
18:33:58.361 [Thread-3] INFO  ru.usermng.dao.SalaryDao - executeUpdate
18:33:58.361 [Thread-4] INFO  ru.usermng.dao.SalaryDao - executeUpdate
18:33:58.362 [Thread-4] INFO  ru.usermng.dao.SalaryDao - executeUpdate done

Видно, что поток Thread-4 выполнил update
Thread-3 заблокирован на выполнении update-а

18:34:08.362 [Thread-4] INFO  ru.usermng.dao.SalaryDao - before commit, timeOut:false

Thread-4 отвалился по таймауту и сделал commit

18:34:08.364 [Thread-3] INFO  ru.usermng.dao.SalaryDao - executeUpdate done
18:34:08.364 [Thread-3] INFO  ru.usermng.dao.SalaryDao - before commit, timeOut:true

Thread-3 разблокировался и завершил update
 */

    void update(DataSource ds, SalaryDao salaryDao, long id, int addValue) {
        try (var connection = ds.getConnection()) {
            salaryDao.update(connection, id, addValue);
            connection.commit();
        } catch (Exception ex) {
            logger.error("err:{}", ex.getMessage(), ex);
        }
    }
}