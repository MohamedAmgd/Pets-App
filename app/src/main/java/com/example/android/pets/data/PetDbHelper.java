package com.example.android.pets.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.pets.data.PetContract.PetEntry;

public class PetDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "pet.db";

    public static final String SQL_CREATE_PETS_TABLE =
            "CREATE TABLE " + PetContract.PetEntry.TABLE_NAME + " (" +
                    PetEntry.COLUMN_ID_NAME + " " + PetContract.PetEntry.COLUMN_ID_TYPE + " PRIMARY KEY AUTOINCREMENT, " +
                    PetContract.PetEntry.COLUMN_NAME_NAME + " " + PetEntry.COLUMN_NAME_TYPE + ", " +
                    PetContract.PetEntry.COLUMN_BREED_NAME + " " + PetEntry.COLUMN_BREED_TYPE + ", " +
                    PetContract.PetEntry.COLUMN_GENDER_NAME + " " + PetContract.PetEntry.COLUMN_GENDER_TYPE + "," +
                    PetContract.PetEntry.COLUMN_WEIGHT_NAME + " " + PetEntry.COLUMN_WEIGHT_TYPE + " );" ;

    public static final String SQL_DROP_PETS_TABLE = "DROP TABLE " + PetContract.PetEntry.TABLE_NAME + ";";

    public PetDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_PETS_TABLE);//create sql
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_PETS_TABLE);//delete sql
        onCreate(db);
    }
}
