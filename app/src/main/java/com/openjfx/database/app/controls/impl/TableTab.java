package com.openjfx.database.app.controls.impl;

import com.jfoenix.controls.JFXButton;
import com.openjfx.database.DML;
import com.openjfx.database.TableColumnMetaHelper;
import com.openjfx.database.app.TableDataHelper;
import com.openjfx.database.app.controls.BaseTab;
import com.openjfx.database.app.controls.TableDataCell;
import com.openjfx.database.app.controls.TableDataView;
import com.openjfx.database.app.enums.NotificationType;
import com.openjfx.database.app.model.TableDataChangeMode;
import com.openjfx.database.app.model.impl.TableTabModel;
import com.openjfx.database.app.utils.AlertUtils;
import com.openjfx.database.app.utils.AssetUtils;
import com.openjfx.database.app.utils.DialogUtils;
import com.openjfx.database.app.utils.FXStringUtils;
import com.openjfx.database.base.AbstractDataBasePool;
import com.openjfx.database.common.utils.StringUtils;
import com.openjfx.database.model.TableColumnMeta;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.openjfx.database.app.DatabaseFX.DATABASE_SOURCE;
import static com.openjfx.database.app.utils.AssetUtils.getLocalImage;
import static com.openjfx.database.common.config.StringConstants.NULL;


/**
 * table tab
 *
 * @author yangkui
 * @since 1.0
 */
public class TableTab extends BaseTab<TableTabModel> {
    /**************************************************************
     *                          布局属性                           *
     **************************************************************/
    private BorderPane borderPane = new BorderPane();

    private TableDataView tableView = new TableDataView();

    private HBox bottomBox = new HBox();

    private HBox leftBox = new HBox();

    private HBox rightBox = new HBox();

    /**********************************************************
     *                       连接池信息                         *
     **********************************************************/
    private final AbstractDataBasePool pool;
    /*******************************************************************
     *                       分页查询参数                                *
     *******************************************************************/

    private int pageIndex = 1;
    private int pageSize = 100;


    /*********************************************************************
     *                              控制按钮                               *
     *********************************************************************/
    private JFXButton addData = new JFXButton();

    private JFXButton flush = new JFXButton();

    private JFXButton next = new JFXButton();

    private JFXButton last = new JFXButton();

    private JFXButton submit = new JFXButton();

    private JFXButton reduce = new JFXButton();

    private TextField numberTextField = new TextField(String.valueOf(pageSize));

    private List<TableColumnMeta> metas = new ArrayList<>();

    private Label totalLabel = new Label("0行数据");

    private final Label flag = new Label();

    /*************************************************************************************
     *                                图标信息                                             *
     *************************************************************************************/
    private static final Image ADD_DATA_ICON = getLocalImage(25, 25, "add_data.png");
    private static final Image FLUSH_ICON = getLocalImage(25, 25, "flush_icon.png");
    private static final Image NEXT_ICON = getLocalImage(20, 20, "next_icon.png");
    private static final Image LAST_ICON = getLocalImage(20, 20, "last_icon.png");
    private static final Image SUBMIT_ICON = getLocalImage(25, 25, "save_icon.png");
    private static final Image REDUCE_ICON = getLocalImage(30, 30, "reduce_icon.png");
    private static final Image FLAG_IMAGE = getLocalImage(20, 20, "point.png");

    /**
     * css样式路径
     */
    private static final String STYLE_SHEETS = "table_tab.css";

    /**
     * 当前表的key值
     */
    private TableColumnMeta keyMeta = null;

    /**
     * 新数据
     */
    private ObservableList<StringProperty> newData = null;


    public TableTab(TableTabModel model) {
        super(model);
        pool = DATABASE_SOURCE.getDataBaseSource(model.getUuid());
        init();
    }

    /**
     * 初始化数据
     */
    private void init() {
        //初始化图标
        flag.setGraphic(new ImageView(FLAG_IMAGE));
        addData.setGraphic(new ImageView(ADD_DATA_ICON));
        flush.setGraphic(new ImageView(FLUSH_ICON));
        next.setGraphic(new ImageView(NEXT_ICON));
        last.setGraphic(new ImageView(LAST_ICON));
        submit.setGraphic(new ImageView(SUBMIT_ICON));
        reduce.setGraphic(new ImageView(REDUCE_ICON));

        addData.setTooltip(new Tooltip("新增数据"));
        flush.setTooltip(new Tooltip("刷新"));
        submit.setTooltip(new Tooltip("保存更改"));
        reduce.setTooltip(new Tooltip("删除"));

        leftBox.getChildren().addAll(addData, reduce, submit);
        rightBox.getChildren().addAll(totalLabel, last, next, numberTextField, flush);

        HBox.setHgrow(rightBox, Priority.ALWAYS);
        bottomBox.getChildren().addAll(leftBox, rightBox);

        bottomBox.getStyleClass().add("bottom-box");

        flush.setOnAction(e -> loadData());

        borderPane.getStylesheets().add(AssetUtils.getCssStyle(STYLE_SHEETS));

        submit.setOnAction(e -> saveChange(false));

        tableView.isChangeProperty().addListener((observable, oldValue, newValue) -> {
            if (Objects.nonNull(newValue) && newValue) {
                setGraphic(flag);
            } else {
                setGraphic(null);
            }
        });

        numberTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                String text = numberTextField.getText();
                if (StringUtils.isEmpty(text)) {
                    numberTextField.setText(String.valueOf(pageSize));
                } else {
                    pageSize = Integer.parseInt(text);
                }
            }
        });

        next.setOnAction(e -> {
            pageIndex++;
            loadData();
        });

        last.setOnAction(e -> {
            if (pageIndex > 1) {
                pageIndex--;
                loadData();
            }
        });

        addData.setOnAction(e -> {
            newData = FXCollections.observableArrayList();
            metas.forEach(meta -> newData.add(new SimpleStringProperty(NULL)));
            tableView.addNewRow(newData);
            tableView.scrollTo(newData);
            tableView.getSelectionModel().select(newData);
        });

        reduce.setOnAction(e -> {
            var selectIndex = tableView.getSelectionModel().getSelectedIndex();
            if (selectIndex == -1) {
                return;
            }
            var item = tableView.getSelectionModel().getSelectedItem();
            tableView.addDeleteItem(item);
        });

        borderPane.setCenter(tableView);
        borderPane.setBottom(bottomBox);

        setContent(borderPane);

        setText(model.getTableName());

        loadTableMeta();
    }


    private void loadTableMeta() {
        Future<List<TableColumnMeta>> future = pool.getDql().showColumns(model.getTable());
        future.onSuccess(metas ->

        {
            int i = 0;
            for (TableColumnMeta meta : metas) {
                createColumn(i, meta.getField());
                i++;
            }
            if (this.metas.size() > 0) {
                this.metas.clear();
            }
            this.metas.addAll(metas);
            Optional<TableColumnMeta> optional = TableColumnMetaHelper.getTableKey(metas);
            if (optional.isPresent()) {
                tableView.setEditable(true);
                keyMeta = optional.get();
            } else {
                DialogUtils.showNotification("当前设计表无Key,无法进行更新操作", Pos.TOP_CENTER, NotificationType.WARNING);
            }
            loadData();
        });
        future.onFailure(t -> DialogUtils.showErrorDialog(t, "获取表信息失败"));
    }

    private void createColumn(final int columnIndex, String title) {
        TableColumn<ObservableList<StringProperty>, String> column = new TableColumn<>();
        column.setText(title);
        column.setCellValueFactory(cellDataFeatures -> {
            ObservableList<StringProperty> values = cellDataFeatures.getValue();
            if (columnIndex >= values.size()) {
                return new SimpleStringProperty("");
            } else {
                return cellDataFeatures.getValue().get(columnIndex);
            }
        });
        column.setCellFactory(TableDataCell.forTableColumn());
        Platform.runLater(() -> this.tableView.getColumns().add(column));
    }

    /**
     * 分页加载数据
     */
    private void loadData() {
        if (tableView.isChange()) {
            Optional<ButtonType> optional = AlertUtils.showConfirm("检测到数据已经更新是否同步到数据库?");
            if (optional.isPresent()) {
                ButtonType buttonType = optional.get();
                if (buttonType == ButtonType.OK) {
                    saveChange(true);
                    return;
                } else {
                    tableView.resetChange();
                }
            }
        }
        //清空之前的数据
        Platform.runLater(() -> {
            tableView.getItems().clear();
        });
        //加载数据
        Future<List<Object[]>> future = pool.getDql().query(model.getTable(), pageIndex, pageSize);
        future.onSuccess(rs -> {
            if (rs.isEmpty()) {
                Platform.runLater(() -> tableView.setPlaceholder(null));
                return;
            }

            List<ObservableList<StringProperty>> list = FXCollections.observableArrayList();

            for (Object[] objects : rs) {
                ObservableList<StringProperty> item = FXCollections.observableArrayList();

                for (Object object : objects) {
                    String val = object.toString();
                    item.add(new SimpleStringProperty(val));
                }

                list.add(item);
            }

            Platform.runLater(() -> {
                tableView.getItems().addAll(list);
                tableView.refresh();
            });

            countDataNumber();
        });
        future.onFailure(t -> DialogUtils.showErrorDialog(t, "加载数据失败"));
    }

    /**
     * 保存更改数据
     *
     * @param isLoading 是否重新加载数据
     */
    private void saveChange(boolean isLoading) {
        DML dml = DATABASE_SOURCE
                .getDataBaseSource(model.getUuid()).getDml();
        Future<Integer> future = newData(dml)
                .compose(rs -> updateData(dml))
                .compose(rs -> deleteData(dml));

        future.onSuccess(rs -> {
            Platform.runLater(() -> {
                if (isLoading) {
                    loadData();
                }
                countDataNumber();
                tableView.resetChange();
                tableView.refresh();
            });
        });
        future.onFailure(t -> DialogUtils.showErrorDialog(t, "更新失败"));
    }

    /**
     * 新增数据
     *
     * @param dml dml
     * @return 返回新增结果
     */
    private Future newData(DML dml) {
        List<ObservableList<StringProperty>> newRows = tableView.getNewRows();

        List<Future> futures = new ArrayList<>();

        for (ObservableList<StringProperty> newRow : newRows) {
            Object[] columns = TableDataHelper.fxPropertyToObject(newRow);
            Future<Long> future = dml.insert(metas, columns, model.getTable());
            var optional = dml.getAutoIncreaseField(metas);
            if (optional.isPresent()) {
                int i = metas.indexOf(optional.get());
                //成功后回调处理自增id
                future.setHandler(ar -> {
                    Platform.runLater(() -> {
                        int index = tableView.getItems().indexOf(newRow);
                        if (index != -1) {
                            var item = tableView.getItems().get(index);
                            item.set(i, new SimpleStringProperty(String.valueOf(ar.result())));
                        }
                    });
                });
            }
            futures.add(future);
        }
        if (futures.size() > 0) {
            return CompositeFuture.all(futures);
        }
        return Future.succeededFuture();
    }

    /**
     * 更细数据
     *
     * @param dml dml
     * @return 返回更新结果
     */
    private Future<Integer> updateData(DML dml) {
        List<TableDataChangeMode> change = tableView.getChangeModes();
        //更新数据
        if (change.size() > 0) {
            int keyIndex = metas.indexOf(keyMeta);

            //由于异步问题可能只能走批量更新,无法走单条更新
            List<Map<String, Object[]>> values = TableDataHelper
                    .getChangeValue(change, metas, keyIndex, tableView.getItems());
            //异步更新数据
            return dml.batchUpdate(values, model.getTable(), metas);
        }
        return Future.succeededFuture();
    }

    private Future deleteData(DML dml) {
        List<ObservableList<StringProperty>> list = tableView.getDeletes();
        if (!list.isEmpty()) {
            int index = metas.indexOf(keyMeta);
            Object[] keys = list.stream()
                    .map(it -> it.get(index))
                    .map(TableDataHelper::singleFxPropertyToObject)
                    .toArray();
            return dml.batchDelete(keyMeta, keys, model.getTable());
        }
        return Future.succeededFuture();
    }


    private void countDataNumber() {
        Future<Long> future = pool.getDql().count(model.getTable());
        future.onSuccess(number -> Platform.runLater(() -> totalLabel.setText(number + "行数据")));
        future.onFailure(t -> DialogUtils.showErrorDialog(t, "统计数据失败"));
    }
}
