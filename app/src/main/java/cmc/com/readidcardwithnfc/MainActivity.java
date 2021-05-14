package cmc.com.readidcardwithnfc;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardService;

import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.lds.CardSecurityFile;
import org.jmrtd.lds.DisplayedImageInfo;
import org.jmrtd.lds.PACEInfo;
import org.jmrtd.lds.SecurityInfo;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.icao.DG3File;
import org.jmrtd.lds.icao.DG5File;
import org.jmrtd.lds.icao.DG7File;
import org.jmrtd.lds.icao.MRZInfo;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;
import org.jmrtd.lds.iso19794.FingerImageInfo;
import org.jmrtd.lds.iso19794.FingerInfo;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cmc.com.readidcardwithnfc.model.AdditionalPersonDetails;
import cmc.com.readidcardwithnfc.model.DocType;
import cmc.com.readidcardwithnfc.model.EDocument;
import cmc.com.readidcardwithnfc.model.PersonDetails;
import cmc.com.readidcardwithnfc.model.PersonToDart;
import cmc.com.readidcardwithnfc.util.DateUtil;
import cmc.com.readidcardwithnfc.util.Image;
import cmc.com.readidcardwithnfc.util.ImageUtil;

import static org.jmrtd.PassportService.DEFAULT_MAX_BLOCKSIZE;
import static org.jmrtd.PassportService.NORMAL_MAX_TRANCEIVE_LENGTH;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int APP_CAMERA_ACTIVITY_REQUEST_CODE = 150;
    private static final int APP_SETTINGS_ACTIVITY_REQUEST_CODE = 550;

    private NfcAdapter adapter;

    private View mainLayout;
    private View loadingLayout;
    private View imageLayout;
    private Button scanIdCard, scanPassport, read;
    private TextView tvResult;
    private ImageView ivPhoto;

    private String doccumentNumber, expirationDate, birthDate;
    private DocType docType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_title);

        mainLayout = findViewById(R.id.main_layout);//chứa ảnh đại diện của tài khoản id card mặc định không được hiển thị
        loadingLayout = findViewById(R.id.loading_layout);//Chứa loading chờ load dữ liệu, mặc định không hiển thị
        imageLayout = findViewById(R.id.image_layout);//Chứa hình ảnh về find id card, mặc định gone
        ivPhoto = findViewById(R.id.view_photo);
        tvResult = findViewById(R.id.text_result);
        scanIdCard = findViewById(R.id.btn_scan_id_card);
        scanIdCard.setOnClickListener(this);
        scanPassport = findViewById(R.id.btn_scan_passport);
        scanPassport.setOnClickListener(this);
        read = findViewById(R.id.btn_read);
        read.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_read:
                readCard();
                break;
            default:
                break;
        }
    }

    private void readCard() {
        String mrzData = "IDVNM0510019778031051001977<<8" +
                "5108185M9912315VNM<<<<<<<<<<<0" +
                "DAM<<QUANG<LANG<<<<<<<<<<<<<<<";
        MRZInfo mrzInfo = new MRZInfo(mrzData);
        setMrzData(mrzInfo);
    }

    private void setMrzData(MRZInfo mrzInfo) {
        adapter = NfcAdapter.getDefaultAdapter(this);
        mainLayout.setVisibility(View.GONE);
        imageLayout.setVisibility(View.VISIBLE);

        doccumentNumber = mrzInfo.getDocumentNumber();
        Log.d("hau", "doccumentNumber: " + doccumentNumber);
        expirationDate = mrzInfo.getDateOfExpiry();
        Log.d("hau", "DateOfExpiry: " + expirationDate);
        birthDate = mrzInfo.getDateOfBirth();
        Log.d("hau", "DateOfBirth: " + birthDate);
        String personalNumber = mrzInfo.getPersonalNumber();
        Log.d("hau", "PersonalNumber: " + personalNumber);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (adapter != null) {
            Intent intent = new Intent(getApplicationContext(), this.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            String[][] filter = new String[][]{new String[]{"android.nfc.tech.IsoDep"}};
            adapter.enableForegroundDispatch(this, pendingIntent, null, filter);
            Log.d("hau", "---Đã phát hiện thẻ NFC---");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null) {
            adapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
//            Tag tag = intent.getExtras().getParcelable(NfcAdapter.EXTRA_TAG);
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (Arrays.asList(tag.getTechList()).contains("android.nfc.tech.IsoDep")) {
                clearViews();
                if (doccumentNumber != null && !doccumentNumber.isEmpty() && expirationDate != null && !expirationDate.isEmpty() && birthDate != null && !birthDate.isEmpty()) {
                    BACKeySpec bacKey = new BACKey(doccumentNumber, birthDate, expirationDate);
                    Log.d("hau", "message: " + IsoDep.get(tag).toString());
                    new ReadNfcTag(IsoDep.get(tag), bacKey).execute();
                    mainLayout.setVisibility(View.GONE);
                    imageLayout.setVisibility(View.GONE);
                    loadingLayout.setVisibility(View.VISIBLE);
                } else {
                    Snackbar.make(loadingLayout, R.string.error_input, Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }

    private class ReadNfcTag extends AsyncTask<Void, Void, Exception> {
        private IsoDep isoDep;
        private BACKeySpec bacKey;

        private ReadNfcTag(IsoDep isoDep, BACKeySpec bacKey) {
            this.isoDep = isoDep;
            this.bacKey = bacKey;
        }

        EDocument eDocument = new EDocument();
        DocType docType = DocType.OTHER;
        PersonDetails personDetails = new PersonDetails();
        PersonToDart personToDart = new PersonToDart();
        AdditionalPersonDetails additionalPersonDetails = new AdditionalPersonDetails();

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                CardService cardService = CardService.getInstance(isoDep);
                cardService.open();

                PassportService service = new PassportService(cardService, NORMAL_MAX_TRANCEIVE_LENGTH, DEFAULT_MAX_BLOCKSIZE, false, true);
                service.open();

                boolean paceSucceeded = false;
                try {
                    CardSecurityFile cardSecurityFile = new CardSecurityFile(service.getInputStream(PassportService.SFI_CARD_ACCESS));
                    Collection<SecurityInfo> securityInfoCollection = cardSecurityFile.getSecurityInfos();
                    for (SecurityInfo securityInfo : securityInfoCollection) {
                        if (securityInfo instanceof PACEInfo) {
                            PACEInfo paceInfo = (PACEInfo) securityInfo;
                            service.doPACE(bacKey, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()), null);
                            paceSucceeded = true;
                        }
                    }
                } catch (Exception e) {
                    Log.d("hau", e.getMessage());
                    Log.w(TAG, e);
                }
                service.sendSelectApplet(paceSucceeded);

                if (!paceSucceeded) {
                    try {
                        service.getInputStream(PassportService.EF_COM).read();
                    } catch (Exception e) {
                        service.doBAC(bacKey);
                    }
                }
                // -- Personal Details -- //
                CardFileInputStream dg1In = service.getInputStream(PassportService.EF_DG1);
                DG1File dg1File = new DG1File(dg1In);

                MRZInfo mrzInfo = dg1File.getMRZInfo();
                Log.d("hau",mrzInfo.getOptionalData1()+"/"+mrzInfo.getOptionalData2());
                personDetails.setName(mrzInfo.getSecondaryIdentifier().replace("<", " ").trim());
                personDetails.setSurname(mrzInfo.getPrimaryIdentifier().replace("<", " ").trim());
                personDetails.setPersonalNumber(mrzInfo.getPersonalNumber());
                personDetails.setGender(mrzInfo.getGender().toString());
                personDetails.setBirthDate(DateUtil.convertFromMrzDate(mrzInfo.getDateOfBirth()));
                personDetails.setExpiryDate(DateUtil.convertFromMrzDate(mrzInfo.getDateOfExpiry()));
                personDetails.setSerialNumber(mrzInfo.getDocumentNumber());
                personDetails.setNationality(mrzInfo.getNationality());
                personDetails.setIssuerAuthority(mrzInfo.getIssuingState());

                if ("I".equals(mrzInfo.getDocumentCode())) {
                    docType = DocType.ID_CARD;
                } else if ("P".equals(mrzInfo.getDocumentCode())) {
                    docType = DocType.PASSPORT;
                }
                // -- Face Image -- //
                CardFileInputStream dg2In = service.getInputStream(PassportService.EF_DG2);
                DG2File dg2File = new DG2File(dg2In);

                List<FaceInfo> faceInfos = dg2File.getFaceInfos();
                List<FaceImageInfo> allFaceImageInfos = new ArrayList<>();
                for (FaceInfo faceInfo : faceInfos) {
                    allFaceImageInfos.addAll(faceInfo.getFaceImageInfos());
                }

                if (!allFaceImageInfos.isEmpty()) {
                    FaceImageInfo faceImageInfo = allFaceImageInfos.iterator().next();
                    Image image = ImageUtil.getImage(MainActivity.this, faceImageInfo);

                    personDetails.setFaceImage(image.getBitmapImage());
                    personDetails.setFaceImageBase64(image.getBase64Image());

                    //Thiết lập dữ liệu để chuyển sang Flutter plugin

                    personToDart.setFaceImage(image.getBase64Image());
                    personToDart.setFaceImageBase64(image.getBase64Image());

                }
                // -- Fingerprint (if exist)-- //
                try {
                    CardFileInputStream dg3In = service.getInputStream(PassportService.EF_DG3);
                    DG3File dg3File = new DG3File(dg3In);

                    List<FingerInfo> fingerInfos = dg3File.getFingerInfos();
                    List<FingerImageInfo> allFingerImageInfos = new ArrayList<>();
                    for (FingerInfo fingerInfo : fingerInfos) {
                        allFingerImageInfos.addAll(fingerInfo.getFingerImageInfos());
                    }

                    List<Bitmap> fingerprintsImage = new ArrayList<>();

                    if (!allFingerImageInfos.isEmpty()) {

                        for (FingerImageInfo fingerImageInfo : allFingerImageInfos) {
                            Image image = ImageUtil.getImage(MainActivity.this, fingerImageInfo);
                            fingerprintsImage.add(image.getBitmapImage());
                        }
                        personDetails.setFingerprints(fingerprintsImage);
                    }
                    Log.d("hau", "Load DG3 ok");
                } catch (Exception e) {
                    Log.d("hau", "Load DG3 error");
                    Log.w(TAG, e);
                }

                // -- Portrait Picture -- //
                try {
                    CardFileInputStream dg5In = service.getInputStream(PassportService.EF_DG5);
                    DG5File dg5File = new DG5File(dg5In);

                    List<DisplayedImageInfo> displayedImageInfos = dg5File.getImages();
                    if (!displayedImageInfos.isEmpty()) {
                        DisplayedImageInfo displayedImageInfo = displayedImageInfos.iterator().next();
                        Image image = ImageUtil.getImage(MainActivity.this, displayedImageInfo);
                        personDetails.setPortraitImage(image.getBitmapImage());
                        personDetails.setPortraitImageBase64(image.getBase64Image());
                    }
                    Log.d("hau", "Load DG5 ok");
                } catch (Exception e) {
                    Log.d("hau", "Load DG5 error");
                    Log.w(TAG, e);
                }

                // -- Signature (if exist) -- //
                try {

                    CardFileInputStream dg7In = service.getInputStream(PassportService.EF_DG7);
                    DG7File dg7File = new DG7File(dg7In);

                    List<DisplayedImageInfo> signatureImageInfos = dg7File.getImages();
                    if (!signatureImageInfos.isEmpty()) {
                        DisplayedImageInfo displayedImageInfo = signatureImageInfos.iterator().next();
                        Image image = ImageUtil.getImage(MainActivity.this, displayedImageInfo);
                        personDetails.setPortraitImage(image.getBitmapImage());
                        personDetails.setPortraitImageBase64(image.getBase64Image());
                    }
                    Log.d("hau", "Load DG7 ok");
                } catch (Exception e) {
                    Log.d("hau", "Load DG7 error");
                    Log.w(TAG, e);
                }

                eDocument.setDocType(docType);
                eDocument.setPersonDetails(personDetails);
                eDocument.setAdditionalPersonDetails(additionalPersonDetails);
            } catch (Exception e) {
                Log.d("hau", "Lỗi ngoài cùng: " + e.getMessage());
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception exception) {
            mainLayout.setVisibility(View.VISIBLE);
            loadingLayout.setVisibility(View.GONE);

            if (exception == null) {
                setResultToView(eDocument);
            } else {
                Snackbar.make(mainLayout, exception.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void setResultToView(EDocument eDocument) {

        Bitmap image = ImageUtil.scaleImage(eDocument.getPersonDetails().getFaceImage());

        ivPhoto.setImageBitmap(image);

        String result = "NAME: " + eDocument.getPersonDetails().getName() + "\n";
        result += "SURNAME: " + eDocument.getPersonDetails().getSurname() + "\n";
        result += "PERSONAL NUMBER: " + eDocument.getPersonDetails().getPersonalNumber() + "\n";
        result += "GENDER: " + eDocument.getPersonDetails().getGender() + "\n";
        result += "BIRTH DATE: " + eDocument.getPersonDetails().getBirthDate() + "\n";
        result += "EXPIRY DATE: " + eDocument.getPersonDetails().getExpiryDate() + "\n";
        result += "SERIAL NUMBER: " + eDocument.getPersonDetails().getSerialNumber() + "\n";
        result += "NATIONALITY: " + eDocument.getPersonDetails().getNationality() + "\n";
        result += "DOC TYPE: " + eDocument.getDocType().name() + "\n";
        result += "ISSUER AUTHORITY: " + eDocument.getPersonDetails().getIssuerAuthority() + "\n";

        tvResult.setText(result);
    }

    private void clearViews() {
        ivPhoto.setImageBitmap(null);
        tvResult.setText("");
    }
}