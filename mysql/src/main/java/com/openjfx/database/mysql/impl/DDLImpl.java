package com.openjfx.database.mysql.impl;

import com.openjfx.database.DDL;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Row;

public class DDLImpl implements DDL {
    private MySQLPool client;

    public DDLImpl(MySQLPool client) {
        this.client = client;
    }

    @Override
    public Future<Void> dropDatabase(String database) {
        String sql = "DROP DATABASE "+database;
        Promise<Void> promise = Promise.promise();
        client.query(sql).onSuccess(rows->{
            for (Row row : rows) {
                System.out.println(row);
            }
            promise.complete();
        }).onFailure(promise::fail);
        return promise.future();
    }

    @Override
    public Future<Void> dropTable(String table) {
        String sql = "DROP TABLE "+table;
        Promise<Void> promise = Promise.promise();
        client.query(sql).onSuccess(rows->{
            promise.complete();
        }).onFailure(promise::fail);
        return promise.future();
    }

    @Override
    public Future<String> ddl(String tableName) {
        String sql = "show create table "+tableName;
        Promise<String> promise = Promise.promise();
        client.query(sql).onSuccess(rows->{
            String ddl = "";
            for (Row row : rows) {
                ddl = (String) row.getValue(1);
            }
            promise.complete(ddl);
        }).onFailure(promise::fail);
        return promise.future();
    }
}
