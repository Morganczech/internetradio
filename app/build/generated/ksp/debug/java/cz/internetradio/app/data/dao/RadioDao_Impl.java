package cz.internetradio.app.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import cz.internetradio.app.data.entity.RadioEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RadioDao_Impl implements RadioDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RadioEntity> __insertionAdapterOfRadioEntity;

  private final EntityDeletionOrUpdateAdapter<RadioEntity> __deletionAdapterOfRadioEntity;

  private final EntityDeletionOrUpdateAdapter<RadioEntity> __updateAdapterOfRadioEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateFavoriteStatus;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllRadios;

  public RadioDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRadioEntity = new EntityInsertionAdapter<RadioEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `radio_stations` (`id`,`name`,`streamUrl`,`imageUrl`,`description`,`startColorValue`,`endColorValue`,`isFavorite`,`category`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RadioEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getStreamUrl());
        statement.bindString(4, entity.getImageUrl());
        if (entity.getDescription() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDescription());
        }
        statement.bindLong(6, entity.getStartColorValue());
        statement.bindLong(7, entity.getEndColorValue());
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindString(9, entity.getCategory());
      }
    };
    this.__deletionAdapterOfRadioEntity = new EntityDeletionOrUpdateAdapter<RadioEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `radio_stations` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RadioEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfRadioEntity = new EntityDeletionOrUpdateAdapter<RadioEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `radio_stations` SET `id` = ?,`name` = ?,`streamUrl` = ?,`imageUrl` = ?,`description` = ?,`startColorValue` = ?,`endColorValue` = ?,`isFavorite` = ?,`category` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RadioEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getStreamUrl());
        statement.bindString(4, entity.getImageUrl());
        if (entity.getDescription() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDescription());
        }
        statement.bindLong(6, entity.getStartColorValue());
        statement.bindLong(7, entity.getEndColorValue());
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindString(9, entity.getCategory());
        statement.bindString(10, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateFavoriteStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE radio_stations SET isFavorite = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllRadios = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM radio_stations";
        return _query;
      }
    };
  }

  @Override
  public Object insertRadio(final RadioEntity radio, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRadioEntity.insert(radio);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRadio(final RadioEntity radio, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfRadioEntity.handle(radio);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRadio(final RadioEntity radio, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfRadioEntity.handle(radio);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateFavoriteStatus(final String radioId, final boolean isFavorite,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateFavoriteStatus.acquire();
        int _argIndex = 1;
        final int _tmp = isFavorite ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, radioId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateFavoriteStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllRadios(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllRadios.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllRadios.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<RadioEntity>> getAllRadios() {
    final String _sql = "SELECT * FROM radio_stations";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"radio_stations"}, new Callable<List<RadioEntity>>() {
      @Override
      @NonNull
      public List<RadioEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfStartColorValue = CursorUtil.getColumnIndexOrThrow(_cursor, "startColorValue");
          final int _cursorIndexOfEndColorValue = CursorUtil.getColumnIndexOrThrow(_cursor, "endColorValue");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final List<RadioEntity> _result = new ArrayList<RadioEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RadioEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpImageUrl;
            _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final long _tmpStartColorValue;
            _tmpStartColorValue = _cursor.getLong(_cursorIndexOfStartColorValue);
            final long _tmpEndColorValue;
            _tmpEndColorValue = _cursor.getLong(_cursorIndexOfEndColorValue);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            _item = new RadioEntity(_tmpId,_tmpName,_tmpStreamUrl,_tmpImageUrl,_tmpDescription,_tmpStartColorValue,_tmpEndColorValue,_tmpIsFavorite,_tmpCategory);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<RadioEntity>> getFavoriteRadios() {
    final String _sql = "SELECT * FROM radio_stations WHERE isFavorite = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"radio_stations"}, new Callable<List<RadioEntity>>() {
      @Override
      @NonNull
      public List<RadioEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfStartColorValue = CursorUtil.getColumnIndexOrThrow(_cursor, "startColorValue");
          final int _cursorIndexOfEndColorValue = CursorUtil.getColumnIndexOrThrow(_cursor, "endColorValue");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final List<RadioEntity> _result = new ArrayList<RadioEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RadioEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpImageUrl;
            _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final long _tmpStartColorValue;
            _tmpStartColorValue = _cursor.getLong(_cursorIndexOfStartColorValue);
            final long _tmpEndColorValue;
            _tmpEndColorValue = _cursor.getLong(_cursorIndexOfEndColorValue);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            _item = new RadioEntity(_tmpId,_tmpName,_tmpStreamUrl,_tmpImageUrl,_tmpDescription,_tmpStartColorValue,_tmpEndColorValue,_tmpIsFavorite,_tmpCategory);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getRadioById(final String radioId,
      final Continuation<? super RadioEntity> $completion) {
    final String _sql = "SELECT * FROM radio_stations WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, radioId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RadioEntity>() {
      @Override
      @Nullable
      public RadioEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfStartColorValue = CursorUtil.getColumnIndexOrThrow(_cursor, "startColorValue");
          final int _cursorIndexOfEndColorValue = CursorUtil.getColumnIndexOrThrow(_cursor, "endColorValue");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final RadioEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpImageUrl;
            _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final long _tmpStartColorValue;
            _tmpStartColorValue = _cursor.getLong(_cursorIndexOfStartColorValue);
            final long _tmpEndColorValue;
            _tmpEndColorValue = _cursor.getLong(_cursorIndexOfEndColorValue);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            _result = new RadioEntity(_tmpId,_tmpName,_tmpStreamUrl,_tmpImageUrl,_tmpDescription,_tmpStartColorValue,_tmpEndColorValue,_tmpIsFavorite,_tmpCategory);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getStationCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM radio_stations";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
