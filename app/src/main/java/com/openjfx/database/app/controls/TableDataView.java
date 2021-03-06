package com.openjfx.database.app.controls;


import com.openjfx.database.app.model.TableDataChangeMode;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.util.*;

/**
 * 自定义表格视图
 *
 * @author yangkui
 * @since 1.0
 */
public class TableDataView extends TableView<ObservableList<StringProperty>> {

    /**
     * 缓存被删除的行数据
     */
    private List<ObservableList<StringProperty>> deletes = FXCollections.observableArrayList();

    /**
     * 将更改的列信息存储到集合之中
     */
    private List<TableDataChangeMode> changeModes = new ArrayList<>();

    /**
     * 新建的行数据
     */
    private List<ObservableList<StringProperty>> newRows = new ArrayList<>();

    /**
     * 当前表数据是否改变
     */
    private BooleanProperty change = new SimpleBooleanProperty();


    public TableDataView() {
        //禁用排序
        setSortPolicy(callback -> null);
    }


    /**
     * 将被删除的行数添加到缓存集合之中
     *
     * @param item 目标数据
     */
    public void addDeleteItem(ObservableList<StringProperty> item) {
        if (deletes.contains(item)) {
            return;
        }
        int index = getItems().indexOf(item);
        boolean a = getItems().remove(item);
        if (a) {

            //检查被删除数据是否在改动列表之中
            var optional = changeModes.stream()
                    .filter(i -> i.getRowIndex() == index)
                    .findAny();

            optional.ifPresent(changeModes::remove);

            deletes.add(item);
            sortChange(index);
            updateChange();
        }
    }

    public void addChangeMode(TableDataChangeMode mode) {
        changeModes.add(mode);
        updateChange();
    }

    public Optional<TableDataChangeMode> getChangeModel(int rowIndex, int columnIndex) {
        String key = rowIndex + "_" + columnIndex;
        Optional<TableDataChangeMode> optional = Optional.empty();
        for (TableDataChangeMode changeMode : changeModes) {
            if (changeMode.getId().equals(key)) {
                optional = Optional.of(changeMode);
                break;
            }
        }
        return optional;
    }

    public void removeChange(TableDataChangeMode model) {
        this.changeModes.remove(model);
        updateChange();
    }

    public void addNewRow(ObservableList<StringProperty> newRow) {
        if (newRows.contains(newRow)) {
            return;
        }
        newRows.add(newRow);
        getItems().add(newRow);
        updateChange();
    }

    public void removeRow(ObservableList<StringProperty> row) {
        this.newRows.remove(row);
        updateChange();
    }

    /**
     * 更新改变状态
     */
    private void updateChange() {
        boolean a = (changeModes.isEmpty() && deletes.isEmpty() && newRows.isEmpty());
        if (!a && !isChange()) {
            setIsChange(true);
        }

        if (a && isChange()) {
            setIsChange(false);
        }
        //刷新视图
        refresh();
    }

    /**
     * 重置缓存信息
     */
    public void resetChange() {
        this.deletes.clear();
        this.changeModes.clear();
        this.newRows.clear();
        this.setIsChange(false);
    }

    /**
     * 重新排序
     *
     * @param index 被删除行系数
     */
    private void sortChange(int index) {
        changeModes.stream()
                .filter(item -> item.getRowIndex() > index)
                .forEach(item -> item.setRowIndex(item.getRowIndex() - 1));
    }

    public boolean isChange() {
        return change.get();
    }

    public BooleanProperty changeProperty() {
        return change;
    }

    public BooleanProperty isChangeProperty() {
        return change;
    }

    public void setIsChange(boolean isChange) {
        this.change.set(isChange);
    }

    public List<ObservableList<StringProperty>> getDeletes() {
        return deletes;
    }

    public List<TableDataChangeMode> getChangeModes() {
        return changeModes;
    }

    public List<ObservableList<StringProperty>> getNewRows() {
        return newRows;
    }
}
