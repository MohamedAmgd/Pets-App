package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.example.android.pets.data.PetContract.PetEntry;

public class PetProvider extends ContentProvider {

    /**
     * URI matcher code for the content URI for the pets table
     */
    private static final int PETS = 100;

    /**
     * URI matcher code for the content URI for a single pet in the pets table
     */
    private static final int PET_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);


    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);
    }

    /**
     * Database helper object
     */
    private PetDbHelper mDbHelper;

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        mDbHelper = new PetDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI.
     * Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query(@NonNull Uri uri
            , String[] projection
            , String selection
            , String[] selectionArgs
            , String sortOrder) {

        // Create and/or open a database to read from it
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor = null;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                // For the PETS code, query the pets table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the pets table.
                cursor = db.query(PetContract.PetEntry.TABLE_NAME
                        , projection
                        , selection
                        , selectionArgs
                        , null
                        , null
                        , sortOrder);
                break;

            case PET_ID:
                // For the PET_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.pets/pets/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = PetContract.PetEntry.COLUMN_ID_NAME + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};


                // This will perform a query on the pets table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = db.query(PetContract.PetEntry.TABLE_NAME
                        , projection
                        , selection
                        , selectionArgs
                        , null
                        , null
                        , sortOrder);
                break;

            default:
                throw new IllegalArgumentException("Cannot query unknown Uri " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(@NonNull Uri uri) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return insertPet(uri, values);
            default:
                throw new IllegalArgumentException("Cannot query unknown Uri");
        }
    }

    /**
     * Insert a pet into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertPet(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(PetEntry.COLUMN_NAME_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Pet requires a name");
        }

        // Check that the gender is valid
        Integer gender = values.getAsInteger(PetEntry.COLUMN_GENDER_NAME);
        if (gender == null || !PetEntry.isValidGender(gender)) {
            throw new IllegalArgumentException("Pet requires valid gender");
        }

        // Check that the weight is provided , is not a negative value (it can be 0 if it is unknown)
        Integer weight = values.getAsInteger(PetEntry.COLUMN_WEIGHT_NAME);
        if (weight != null && weight < 0) {
            throw new IllegalArgumentException("Pet requires valid weight");
        }

        // Get writeable database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Insert the new pet with the given values
        long id = db.insert(PetContract.PetEntry.TABLE_NAME, null, values);

        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }


    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                // Delete all rows that match the selection and selection args
                return deletePet(uri, selection, selectionArgs);
            case PET_ID:
                // Delete a single row given by the ID in the URI
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return deletePet(uri, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }

    private int deletePet(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Delete the pet(s) with the given selection
        int numRows = db.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (numRows > 0)
            getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows deleted
        return numRows;
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return updatePet(uri, values, selection, selectionArgs);
            case PET_ID:
                // For the PET_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // If the {@link PetEntry#COLUMN_NAME_NAME} key is present,
        // Check that the name is not null
        if (values.containsKey(PetEntry.COLUMN_NAME_NAME)) {
            String name = values.getAsString(PetEntry.COLUMN_NAME_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }

        // If the {@link PetEntry#COLUMN_GENDER_NAME} key is present,
        // Check that the gender is valid
        if (values.containsKey(PetEntry.COLUMN_GENDER_NAME)) {
            Integer gender = values.getAsInteger(PetEntry.COLUMN_GENDER_NAME);
            if (gender == null || !PetEntry.isValidGender(gender)) {
                throw new IllegalArgumentException("Pet requires valid gender");
            }
        }

        // If the {@link PetEntry#COLUMN_WEIGHT_NAME} key is present,
        // Check that the weight is provided , is not a negative value (it can be 0 if it is unknown)
        if (values.containsKey(PetEntry.COLUMN_WEIGHT_NAME)) {
            Integer weight = values.getAsInteger(PetEntry.COLUMN_WEIGHT_NAME);
            if (weight != null && weight < 0) {
                throw new IllegalArgumentException("Pet requires valid weight");
            }
        }

        // Get writeable database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Update the pet(s) with the new given values
        int numRows = db.update(PetContract.PetEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (numRows > 0)
            getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of database rows affected by the update statement
        return numRows;
    }
}
