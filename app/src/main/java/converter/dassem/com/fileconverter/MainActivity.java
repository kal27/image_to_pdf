package converter.dassem.com.fileconverter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final int FILE_CODE = 1337;
    private final int DIR_CODE = 1339;
    private String currentFilename = "", path = "";
    private Bitmap bitmap = null;
    private ProgressDialog progressDialog;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private MaterialBetterSpinner conversion_Type;
    private boolean conversionTypePNG = true;
    private EditText filenameInput;
    private String filenameIn = "temp";
    private Pattern pattern;
    private TextView currentlyLoadedFile;
    private int pageCount = 0;
    private MainActivity mainActivity;
    private MaterialBetterSpinner current_image_quality, current_page_size;
    private boolean lowImageQuality = true;
    private String pagesize = "default";
    private ArrayList<Uri> imagePaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        filenameInput = (EditText) findViewById(R.id.filename_input);
        String filenameRegex = "^[a-zA-Z0-9]+$";
        pattern = Pattern.compile(filenameRegex);
        currentlyLoadedFile = (TextView) findViewById(R.id.currentFile);

        current_image_quality = (MaterialBetterSpinner) findViewById(R.id.imageQuality);
        String[] quality = getResources().getStringArray(R.array.imageQuality);
        ArrayAdapter qualities = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, quality);
        current_image_quality.setAdapter(qualities);

        current_page_size = (MaterialBetterSpinner) findViewById(R.id.pageSize);
        String[] pageSize = getResources().getStringArray(R.array.pageSize);
        ArrayAdapter page_sizes = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, pageSize);
        current_page_size.setAdapter(page_sizes);

        mainActivity = this;
        filenameInput.addTextChangedListener(new TextValidator(filenameInput) {
            @Override
            public void validate(TextView textView, String text) {
                if (!validateText(text)) {
                    textView.setError("Please enter a valid filename. Allowed characters: letters and numbers.");
                } else {
                    filenameIn = filenameInput.getText().toString();
                }
            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();
    }


    private boolean validateText(String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.open_file) {
            startChooseFileIntent(false);
        }

        return super.onOptionsItemSelected(item);
    }

    private void startChooseFileIntent(boolean isDir) {
        Intent i = new Intent(this, FilePicker.class);
        // This works if you defined the intent filter
        // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

        // Set these depending on your use case. These are the defaults.
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        if (isDir) {
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
        } else {
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
        }

        // Configure initial directory by specifying a String.
        // You could specify a String like "/storage/emulated/0/", but that can
        // dangerous. Always use Android's API calls to get paths to the SD-card or
        // internal memory.
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        if (isDir) {
            startActivityForResult(i, DIR_CODE);
        } else {
            startActivityForResult(i, FILE_CODE);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true)) {
                // For JellyBean and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip = data.getClipData();
                    if (clip != null) {
                        imagePaths = new ArrayList<>();
                        loadFilepath(clip.getItemAt(0).getUri());
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            Uri uri = clip.getItemAt(i).getUri();
                            imagePaths.add(uri);
                        }
                    }
                    // For Ice Cream Sandwich
                } else {
                    ArrayList<String> paths = data.getStringArrayListExtra
                            (FilePickerActivity.EXTRA_PATHS);

                    if (paths != null) {
                        for (String path : paths) {
                            Uri uri = Uri.parse(path);
                            loadFilepath(uri);


                        }
                    }
                }

            } else {
                Uri uri = data.getData();
                loadFilepath(uri);

            }
        } else if (requestCode == DIR_CODE && resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                // For JellyBean and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip = data.getClipData();

                    if (clip != null) {
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            Uri uri = clip.getItemAt(i).getUri();
                            saveFile(uri);

                        }
                    }
                    // For Ice Cream Sandwich
                } else {
                    ArrayList<String> paths = data.getStringArrayListExtra
                            (FilePickerActivity.EXTRA_PATHS);

                    if (paths != null) {
                        for (String path : paths) {
                            Uri uri = Uri.parse(path);
                            saveFile(uri);


                        }
                    }
                }

            } else {
                Uri uri = data.getData();
                saveFile(uri);

            }
        }
    }

    private void loadFilepath(Uri uri) {
        String filename = uri.getLastPathSegment();
        currentFilename = filename;
        currentFilename = currentFilename.replace(".png", "");
        currentFilename = currentFilename.replace(".jpg", "");
        currentFilename = currentFilename.replace(".jpeg", "");
        path = uri.getPath();
        path = path.replace(filename, "");
        currentlyLoadedFile.setText(currentFilename);
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(MainActivity.this);

        // Set progress dialog style horizontal
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        // Set the progress dialog title and message
        progressDialog.setTitle(getString(R.string.loading));
        progressDialog.setMessage(getString(R.string.rendering));

        // Set the progress dialog background color
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FFD4D9D0")));

        progressDialog.setIndeterminate(false);
                /*
                    Set the progress dialog non cancelable
                    It will disallow user's to cancel progress dialog by clicking outside of dialog
                    But, user's can cancel the progress dialog by cancel button
                 */
        progressDialog.setCancelable(false);

        progressDialog.setMax(100);

        // Put a cancel button in progress dialog
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            // Set a click listener for progress dialog cancel button
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dismiss the progress dialog
                progressDialog.dismiss();

                isRunning.set(false);
                // Tell the system about cancellation
            }
        });


        // Set the progress status zero on each button click
    }

    private void renderPages(final String filename) throws IOException {
        pageCount = imagePaths.size();
        progressDialog.setMax(pageCount);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                try {
                    Document document = new Document();
                    PdfWriter.getInstance(document, new FileOutputStream(path + "/" + filename + ".pdf"));
                    document.open();
                    Image image;
                    for (int i = 0; i < pageCount; i++) {
                        Uri uri = imagePaths.get(i);
                        image = Image.getInstance(new File(uri.getPath()).getAbsolutePath());
                        if (lowImageQuality) {
                            image.setCompressionLevel(1);
                        }
                        if (pagesize != null) {
                            if (pagesize.equals("") || (pagesize.equalsIgnoreCase("default"))) {
                                document.setPageSize(image);
                            } else if (pagesize.equalsIgnoreCase("a4")) {
                                document.setPageSize(PageSize.A4);
                                image.scaleToFit(PageSize.A4);
                            } else if (pagesize.equalsIgnoreCase("letter")) {
                                document.setPageSize(PageSize.LETTER);
                                image.scaleToFit(PageSize.LETTER);
                            } else if (pagesize.equalsIgnoreCase("legal")) {
                                document.setPageSize(PageSize.LEGAL);
                                image.scaleToFit(PageSize.LEGAL);
                            }
                        } else {
                            document.setPageSize(image);
                        }
                        document.newPage();
                        image.setAbsolutePosition(0, 0);
                        document.add(image);
                        if (progressDialog.isShowing()) {
                            progressDialog.setProgress(i);
                        }
                        if (!isRunning.get()) {
                            break;
                        }

                    }
                    document.close();
                } catch (DocumentException | IOException e) {
                    Toast.makeText(mainActivity, e.toString(), Toast.LENGTH_SHORT).show();
                }

                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(MainActivity.this, "Files sucessfully saved in " + path, Toast.LENGTH_SHORT).show();
            }

            @Override
            protected void onPreExecute() {
                isRunning.set(true);
                if (progressDialog != null) {
                    if (!progressDialog.isShowing()) {
                        progressDialog.show();
                    }
                }
            }

        }

                .

                        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    private void saveFile(Uri uri) {
        try {
            File dir = null;
            if (uri != null) {
                dir = new File(uri.getPath());
            }

            if (!dir.exists()) {
                dir.mkdirs();
            }
            final File myFile;
            if (conversionTypePNG) {
                myFile = new File(dir, currentFilename + ".png");
            } else {
                myFile = new File(dir, currentFilename + ".jpg");
            }
            path = dir.getPath();

            if (!myFile.exists()) {
                myFile.createNewFile();
            }

            FileOutputStream out = null;
            try {
                out = new FileOutputStream(myFile);
                if (conversionTypePNG) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    makeToast(e.getMessage());
                }
            }
        } catch (IOException e) {
            makeToast(e.getMessage());
        }

    }

    private void saveFile(String pathToFile) {
        FileOutputStream out = null;
        try {
            if (conversionTypePNG) {
                out = new FileOutputStream(pathToFile + "/" + currentFilename + ".png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            } else {
                out = new FileOutputStream(pathToFile + "/" + currentFilename + ".jpg");
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            }
        } catch (Exception e) {
            e.printStackTrace();
            makeToast(e.getMessage());
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                makeToast(e.getMessage());
            }
        }
    }

    private void makeToast(final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

    }


    @Override
    public void onClick(View view) {
        if (imagePaths == null) {
            Toast.makeText(MainActivity.this, "Please open image files first.", Toast.LENGTH_SHORT).show();
        } else if (imagePaths.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please open image files first.", Toast.LENGTH_SHORT).show();
        } else {
            try {
                if ((filenameInput.getText() != null) && (!filenameInput.getText().toString().equals(""))) {
                    filenameIn = filenameInput.getText().toString();
                }
                lowImageQuality = current_image_quality.getText().toString().equalsIgnoreCase("low");
                pagesize = current_page_size.getText().toString();
                showProgressDialog();
                renderPages(filenameIn);


            } catch (IOException ignore) {

            }
        }
    }
}
