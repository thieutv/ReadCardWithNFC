package cmc.com.readidcardwithnfc;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
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

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.util.encoders.Base64;
import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.lds.CardAccessFile;
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo;
import org.jmrtd.lds.DataGroup;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.PACEInfo;
import org.jmrtd.lds.SODFile;
import org.jmrtd.lds.SecurityInfo;
import org.jmrtd.lds.icao.DG11File;
import org.jmrtd.lds.icao.DG12File;
import org.jmrtd.lds.icao.DG14File;
import org.jmrtd.lds.icao.DG15File;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import cmc.com.readidcardwithnfc.model.AdditionalPersonDetails;
import cmc.com.readidcardwithnfc.model.DG13File;
import cmc.com.readidcardwithnfc.model.DocType;
import cmc.com.readidcardwithnfc.model.EDocument;
import cmc.com.readidcardwithnfc.model.OptionPersonal;
import cmc.com.readidcardwithnfc.model.PersonDetails;
import cmc.com.readidcardwithnfc.model.PersonToDart;
import cmc.com.readidcardwithnfc.util.DateUtil;
import cmc.com.readidcardwithnfc.util.Image;
import cmc.com.readidcardwithnfc.util.ImageUtil;
import cmc.com.readidcardwithnfc.util.StringUtil;

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

    //click scan nfc
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

//    String mrzData = "IDVNM0510019778031051001977<<8" +
//            "5108185M9912315VNM<<<<<<<<<<<0" +
//            "DAM<<QUANG<LANG<<<<<<<<<<<<<<<";

    private void readCard() {
        String mrzData = "IDVNM1820117198031182011719<<8" +
                "8207241F4207243VNM<<<<<<<<<<<2" +
                "NGUYEN<<THI<THUY<LOAN<<<<<<<<<";
//
//        String mrzData = "IDVNM0510019778031051001977<<8" +
//                "5108185M9912315VNM<<<<<<<<<<<0" +
//                "DAM<<QUANG<LANG<<<<<<<<<<<<<<<";

//        String mrzData = "IDVNM0900133358031090013335<<8" +
//                "9008191M3008199VNM<<<<<<<<<<<0" +
//                "LE<<XUAN<HUNG<<<<<<<<<<<<<<<<<";
//        String mrzData = "IDVNM0770277778001077027777<<8" +
//                "7709195M3709197VNM<<<<<<<<<<<4" +
//                "Le<<QUANG<HUY<<<<<<<<<<<<<<<<<<<";

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
            Tag tag = intent.getExtras().getParcelable(NfcAdapter.EXTRA_TAG);
            //Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
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

        private DG1File dg1File;
        private DG2File dg2File;
        private DG14File dg14File;
        private SODFile sodFile;
        private DG12File dg12File;
        private DG13File dg13File;
        private DG15File dg15File;
        private boolean chipAuthSucceeded = false;
        private boolean passiveAuthSuccess = false;

        private byte[] dg14Encoded = new byte[0];

        EDocument eDocument = new EDocument();
        DocType docType = DocType.OTHER;

        PersonDetails personDetails = new PersonDetails();
        AdditionalPersonDetails additionalPersonDetails = new AdditionalPersonDetails();

        //giao thức xác thực chip Extended Access Control (EAC).

        /**
         * Xác thực chip là một tính năng an toàn kiểm tra tính xác thực của chip, chống lại việc thay thế nhân bản chip.
         * eMRTD sử dụng cơ chế sau để xác thực chip:
         * 1. Xác thực chủ động (Active authentication) nếu có dữ liệu về khóa công khai của chíp eMRTD trong DG15. Hệ thống kiểm tra và đọc dữ liệu trong DG15
         * và thực hiện xác thực chủ động.
         * 2. Xác thực chip nếu có dữ liệu SecurityInfos trong DG14. Hệ thống kiểm tra sẽ đọc DG14 và thực hiện xác thực chip.
         * 3. Thực hiện PACE với ánh xạ xác thực chip (PACE-CAM) nếu có dữ liệu theo cấu trúc PACEInfo trong file cơ bản CardAccess. Nếu PACE-CAM được thực hiện
         * thành công trong quy trình truy cập chip, thiết bị đầu cuối có thể thực hiện các thao tác sau để xác thực chip.
         * - Xác thực chủ động: là tính năng an toàn ngăn chặn nhân bản chip bằng cách bổ sung thêm cặp khóa dành riêng cho chip:
         * khóa công khai được lưu trong DG15 và được bảo vệ bởi xác thưc thụ động.
         * Khóa riêng tương ứng được lưu trong vùng nhớ an toàn và chỉ có thể được sử dụng bên trong chip eMRTD và không thể đọc từ bên ngoài.
         * Giao thức Challenge-Respond được chip eMRTD sử dụng để chứng minh tri thức về khóa riêng này. Trong giao thức này chip eMRTD ký số một thách
         * thức (Giá trị được chọn ngẫu nhiên bởi thiết bị đầu cuối). Thiết bị đầu cuối kiểm tra ck do eMRTD gửi đến nếu hợp lệ thì xem chip eMRTD là
         * nguyên gốc.
         *
         * @param service
         */
        private void doChipAuth(PassportService service) {
            try {
                CardFileInputStream dg14In = service.getInputStream(PassportService.EF_DG14);
                dg14Encoded = IOUtils.toByteArray(dg14In);
                ByteArrayInputStream dg14InByte = new ByteArrayInputStream(dg14Encoded);
                dg14File = new DG14File(dg14InByte);
                Collection<SecurityInfo> dg14FileSecurityInfos = dg14File.getSecurityInfos();
                for (SecurityInfo securityInfo : dg14FileSecurityInfos) {
                    if (securityInfo instanceof ChipAuthenticationPublicKeyInfo) {
                        ChipAuthenticationPublicKeyInfo publicKeyInfo = (ChipAuthenticationPublicKeyInfo) securityInfo;

                        BigInteger keyId = publicKeyInfo.getKeyId();
                        PublicKey publicKey = publicKeyInfo.getSubjectPublicKey();
                        Log.d("hau1", "==== Public key algorithm: " + publicKey.getAlgorithm());//Public key algorithm: EC
                        //Log.d("hau1", publicKey.getFormat());//X.509
                        String oid = publicKeyInfo.getObjectIdentifier();
                        service.doEACCA(keyId, ChipAuthenticationPublicKeyInfo.ID_CA_ECDH_AES_CBC_CMAC_256, oid, publicKey);
                        chipAuthSucceeded = true;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }

        //Giao thức xác thực chip Passive Authentication (PA).

        /**
         * Thực hiện xác thực thụ động (Bắt buộc) nhằm khẳng định dữ liệu trong chip eMRTD (các nhóm dữ liệu DG1-DG16 và SOD trong cấu trúc dữ liệu LDS)
         * là xác thực (Biết được nguồn gốc của dữ liệu) và toàn ven (Chưa bị thay đổi). vì không yêu cầu khả năng xử lý của chip trong eMRTD, nên được
         * gọi là xác thực thụ động đối với nội dung của chip eMRTD.
         * <p>
         * Cách thực hiện: b1 - đọc đối tượng an toàn tài liệu SOD từ chip eMRTD.
         * b2 - Lấy chứng thư số document signer, chứng thư số CSCA và danh sách thu hồi chứng thư số từ PKD
         * b3 - Kiểm tra chữ ký số Chứng thư số document signer, Chứng thư số CSCA và chữ ký só của đối tượng an toàn tài liệu (SOD)
         * b4 - Tính giá trị băm của các nhóm dữ liệu đã đọc và so sánh chúng với các giá trị băm trong đối tượng an toàn dữ liệu (SOD)
         * Xác thực thủ động cho phép thiết bị đầu cuối phát hiện các nhóm dữ liệu bị sửa đổi. nó sử dụng cks kèm theo hạ tầng mật mã khóa công khai.
         */
        private void doPassiveAuth() {
            try {

                MessageDigest digest = MessageDigest.getInstance(sodFile.getDigestAlgorithm());

                Log.d("hau1", "==== Signature Algorithm: " + sodFile.getDigestEncryptionAlgorithm());
                //Log.d("hau1", "==== Public key algorithm: " + sodFile.getDigestAlgorithm());
                Log.d("hau1", "==== LDS Version: " + sodFile.getLDSVersion());
                //Log.d("hau1", "==== SignerInfoDigestAlgorithm: " + sodFile.getSignerInfoDigestAlgorithm());

                X509Certificate certificates = sodFile.getDocSigningCertificate();
                Log.d("hau1", "==== PublicKey: " + certificates.getPublicKey());
                Log.d("hau1", "==== IssuerDN: " + certificates.getIssuerDN());
                Log.d("hau1", "==== SerialNumber: " + certificates.getSerialNumber());
                Log.d("hau1", "==== SubjectDN: " + certificates.getSubjectDN());
                Log.d("hau1", "==== Version: " + certificates.getVersion());
                Log.d("hau1", "==== Valid from: " + DateUtil.dateToString(certificates.getNotBefore(), new SimpleDateFormat("dd/MM/yyyy")));
                Log.d("hau1", "==== Valid to: " + DateUtil.dateToString(certificates.getNotAfter(), new SimpleDateFormat("dd/MM/yyyy")));
                Log.d("hau1", "==== DocSigningCertificate: " + sodFile.getDocSigningCertificate());

                Map<Integer, byte[]> dataHashes = sodFile.getDataGroupHashes();
                Log.d("hau1", "dataHashes at 1: " + Base64.toBase64String(dataHashes.get(1)));
                Log.d("hau1", "dataHashes at 2: " + dataHashes.get(2));
                Log.d("hau1", "dataHashes at 14: " + dataHashes.get(14));
                byte[] dg14Hash = new byte[0];
                if (chipAuthSucceeded) {
                    dg14Hash = digest.digest(dg14Encoded);
                }
                byte[] dg1Hash = digest.digest(dg1File.getEncoded());
                byte[] dg2Hash = digest.digest(dg2File.getEncoded());
                if (Arrays.equals(dg1Hash, dataHashes.get(1)) && Arrays.equals(dg2Hash, dataHashes.get(2)) && (!chipAuthSucceeded || Arrays.equals(dg14Hash, dataHashes.get(14)))) {
                    // Lấy CSCA từ german master list
                    ASN1InputStream asn1InputStream = new ASN1InputStream(getAssets().open("CSCAVietnam.crt")); //CSCAVietnam.pem
                    //ASN1InputStream asn1InputStream = new ASN1InputStream(getAssets().open("CSCAVietnam.pem"));
                    ASN1Primitive p;
                    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                    keystore.load(null, null);
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    int a = 0;
                    while ((p = asn1InputStream.readObject()) != null) {
                        a++;
                        Log.d("hau2", "giá trị vòng lặp " + a);
                        ASN1Sequence asn1 = ASN1Sequence.getInstance(p);
                        if (asn1 == null || asn1.size() == 0) {
                            throw new IllegalArgumentException("null or empty sequence passed.");
                        }
                        Log.d("hau2", "asn1 size: " + asn1.size());
                        if (asn1.size() != 2) {
                            throw new IllegalArgumentException("Incorrect sequence size: " + asn1.size());
                        }
                        ASN1Set certSet = ASN1Set.getInstance(asn1.getObjectAt(1));

                        for (int i = 0; i < certSet.size(); i++) {
                            Certificate certificate = Certificate.getInstance(certSet.getObjectAt(i));

                            byte[] pemCertificate = certificate.getEncoded();

                            java.security.cert.Certificate javaCertificate = cf.generateCertificate(new ByteArrayInputStream(pemCertificate));
                            keystore.setCertificateEntry(String.valueOf(i), javaCertificate);
                        }
                    }
                    List<X509Certificate> docSigningCertificates = sodFile.getDocSigningCertificates();
                    for (X509Certificate docSigningCertificate : docSigningCertificates) {
                        docSigningCertificate.checkValidity();
                    }
                    //Chúng tôi kiểm tra xem chứng chỉ có được ký bởi CSCA đáng tin cậy hay không
                    // TODO: verify if certificate is revoked (xác mình chứng chỉ có bị thu hồi hay không).
                    CertPath cp = cf.generateCertPath(docSigningCertificates);
                    PKIXParameters pkixParameters = new PKIXParameters(keystore);
                    pkixParameters.setRevocationEnabled(false);
                    CertPathValidator cpv = CertPathValidator.getInstance(CertPathValidator.getDefaultType());
                    cpv.validate(cp, pkixParameters);

                    String sodDigestEncryptionAlgorithm = sodFile.getDigestEncryptionAlgorithm();
                    Log.d("hau1", sodDigestEncryptionAlgorithm);
                    boolean isSSA = false;
                    if (sodDigestEncryptionAlgorithm.equals("SSAwithRSA/PSS")) {
                        sodDigestEncryptionAlgorithm = "SHA256withRSA/PSS";
                        isSSA = true;
                    }
                    Signature sign = Signature.getInstance(sodDigestEncryptionAlgorithm);
                    if (isSSA) {
                        sign.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
                    }
                    sign.initVerify(sodFile.getDocSigningCertificate());
                    sign.update(sodFile.getEContent());
                    passiveAuthSuccess = sign.verify(sodFile.getEncryptedDigest());
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                //Bước 1 Đọc EF.CardAccess
                CardService cardService = CardService.getInstance(isoDep);
                cardService.open();

                // Bước 2,3 4 thực hiện giao thức kiểm soát truy nhập PACE (nếu được hỗ trợ).
                //Chọn ứng dụng ePassport
                //Thực hiện giao thức kiểm soát truy nhập BAC nếu như PACE không được hỗ trợ.

                PassportService service = new PassportService(cardService, NORMAL_MAX_TRANCEIVE_LENGTH, DEFAULT_MAX_BLOCKSIZE, false, true);
                service.open();

                boolean paceSucceeded = false;
                try {
//                    CardSecurityFile cardSecurityFile = new CardSecurityFile(service.getInputStream(PassportService.EF_CARD_ACCESS));//SFI_CARD_ACCESS
//                    Log.d("hau1", "DigestAlgorithm: " + cardSecurityFile.getDigestAlgorithm());
//                    Log.d("hau1", "DigestEncryptionAlgorithm: " + cardSecurityFile.getDigestEncryptionAlgorithm());
//
//                    Collection<SecurityInfo> securityInfoCollection = cardSecurityFile.getSecurityInfos();
//                    for (SecurityInfo securityInfo : securityInfoCollection) {
//                        if (securityInfo instanceof PACEInfo) {
//                            PACEInfo paceInfo = (PACEInfo) securityInfo;
//                            service.doPACE(bacKey, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()), null);
//                            paceSucceeded = true;
//                        }
//                    }
                    CardAccessFile cardAccessFile = new CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS));
                    Collection<SecurityInfo> securityInfoCollection = cardAccessFile.getSecurityInfos();
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
                try {
                    CardFileInputStream dg1In = service.getInputStream(PassportService.EF_DG1);
                    dg1File = new DG1File(dg1In);
                    CardFileInputStream dg2In = service.getInputStream(PassportService.EF_DG2);
                    dg2File = new DG2File(dg2In);
                    CardFileInputStream sodIn = service.getInputStream(PassportService.EF_SOD);
                    sodFile = new SODFile(sodIn);
//

                    CardFileInputStream dg15In = service.getInputStream(PassportService.EF_DG15);
                    dg15File = new DG15File(dg15In);

                    CardFileInputStream dg13In = service.getInputStream(PassportService.EF_DG13);
                    dg13File = new DG13File(dg13In);
                    dg13File.readContent();

                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                //Thực hiện xác thực chủ động (Xác thực chip) tùy chọn có thể thực hiện hoặc không.
                //Thực hiện xác thực chip (perform Chip Authentication) bằng nhóm dữ liệu DG14
                doChipAuth(service);

                //sau đó xác thực thụ (Passive Authentication) bằng SODFile
                doPassiveAuth();

                MRZInfo mrzInfo = dg1File.getMRZInfo();
                Log.d("hau", mrzInfo.getOptionalData1() + "/" + mrzInfo.getOptionalData2());
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


                List<FaceInfo> allFaceInfo = dg2File.getFaceInfos();
                List<FaceImageInfo> allFaceImageInfo = new ArrayList<>();
                for (FaceInfo faceInfo : allFaceInfo) {
                    allFaceImageInfo.addAll(faceInfo.getFaceImageInfos());
                }
                if (!allFaceImageInfo.isEmpty()) {
                    FaceImageInfo faceImageInfo = allFaceImageInfo.iterator().next();
                    Image image = ImageUtil.getImage(MainActivity.this, faceImageInfo);

                    personDetails.setFaceImage(image.getBitmapImage());
                    personDetails.setFaceImageBase64(image.getBase64Image());

                }

                //Đọc dữ liệu DG15
                Log.d("hau1", "==== DG15 Public key: " + dg15File.getPublicKey());
                Log.d("hau1", "==== DG15 Algorithm: " + dg15File.getPublicKey().getAlgorithm());
                Log.d("hau1", "==== DG15 public key format: " + dg15File.getPublicKey().getFormat());
                //đọc dữ liệu dg 12

//                    Log.d("hau1", "==== DG12 Issuing Authority: " + dg12File.getIssuingAuthority());
//                    Log.d("hau1", "==== DG12 Date of Issuing: " + dg12File.getDateOfIssue());
//                    Log.d("hau1", "==== DG12 TaxOrExitRequirements: " + dg12File.getTaxOrExitRequirements());
//                    Log.d("hau1", "==== DG12 DateAndTimeOfPersonalization: " + dg12File.getDateAndTimeOfPersonalization());

                // -- Fingerprint (if exist)-- //
//                try {
//                    CardFileInputStream dg3In = service.getInputStream(PassportService.EF_DG3);
//                    DG3File dg3File = new DG3File(dg3In);
//
//                    List<FingerInfo> fingerInfos = dg3File.getFingerInfos();
//                    List<FingerImageInfo> allFingerImageInfos = new ArrayList<>();
//                    for (FingerInfo fingerInfo : fingerInfos) {
//                        allFingerImageInfos.addAll(fingerInfo.getFingerImageInfos());
//                    }
//
//                    List<Bitmap> fingerprintsImage = new ArrayList<>();
//
//                    if (!allFingerImageInfos.isEmpty()) {
//
//                        for (FingerImageInfo fingerImageInfo : allFingerImageInfos) {
//                            Image image = ImageUtil.getImage(MainActivity.this, fingerImageInfo);
//                            fingerprintsImage.add(image.getBitmapImage());
//                        }
//                        personDetails.setFingerprints(fingerprintsImage);
//                    }
//                    Log.d("hau", "Load DG3 ok");
//                } catch (Exception e) {
//                    Log.d("hau", "Load DG3 error");
//                    Log.w(TAG, e);
//                }

                // -- Portrait Picture -- //
//                try {
//                    CardFileInputStream dg5In = service.getInputStream(PassportService.EF_DG5);
//                    DG5File dg5File = new DG5File(dg5In);
//
//                    List<DisplayedImageInfo> displayedImageInfos = dg5File.getImages();
//                    if (!displayedImageInfos.isEmpty()) {
//                        DisplayedImageInfo displayedImageInfo = displayedImageInfos.iterator().next();
//                        Image image = ImageUtil.getImage(MainActivity.this, displayedImageInfo);
//                        personDetails.setPortraitImage(image.getBitmapImage());
//                        personDetails.setPortraitImageBase64(image.getBase64Image());
//                    }
//                    Log.d("hau", "Load DG5 ok");
//                } catch (Exception e) {
//                    Log.d("hau", "Load DG5 error");
//                    Log.w(TAG, e);
//                }

                // -- Signature (if exist) -- //
//                try {
//
//                    CardFileInputStream dg7In = service.getInputStream(PassportService.EF_DG7);
//                    DG7File dg7File = new DG7File(dg7In);
//
//                    List<DisplayedImageInfo> signatureImageInfos = dg7File.getImages();
//                    if (!signatureImageInfos.isEmpty()) {
//                        DisplayedImageInfo displayedImageInfo = signatureImageInfos.iterator().next();
//                        Image image = ImageUtil.getImage(MainActivity.this, displayedImageInfo);
//                        personDetails.setPortraitImage(image.getBitmapImage());
//                        personDetails.setPortraitImageBase64(image.getBase64Image());
//                    }
//                    Log.d("hau", "Load DG7 ok");
//                } catch (Exception e) {
//                    Log.d("hau", "Load DG7 error");
//                    Log.w(TAG, e);
//                }
                eDocument.setOptionPerson(dg13File.getOptionPersonal());
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
                setResultToView(eDocument, passiveAuthSuccess, chipAuthSucceeded);
            } else {
                Snackbar.make(mainLayout, exception.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void setResultToView(EDocument eDocument, boolean passiveAuthSuccess, boolean chipAuthSucceeded) {

        Bitmap image = ImageUtil.scaleImage(eDocument.getPersonDetails().getFaceImage());

        ivPhoto.setImageBitmap(image);
        String passiveAuthStr = "";
        if (passiveAuthSuccess) {
            passiveAuthStr = getString(R.string.pass);
        } else {
            passiveAuthStr = getString(R.string.failed);
        }
        String chipAuthStr = "";
        if (chipAuthSucceeded) {
            chipAuthStr = getString(R.string.pass);
        } else {
            chipAuthStr = getString(R.string.failed);
        }

        String result = "Họ Và Tên: " + eDocument.getOptionPerson().getName() + "\n";//eDocument.getPersonDetails().getName() + "\n";
        //result += "SURNAME: " + eDocument.getPersonDetails().getSurname() + "\n";
        //result += "PERSONAL NUMBER: " + eDocument.getPersonDetails().getPersonalNumber() + "\n";
        result += "Số Căn Cước Công Dân: " + eDocument.getOptionPerson().getCCCDNumber() + "\n";
        result += "Ngày Sinh: " + eDocument.getOptionPerson().getDateOfBirth() + "\n";
        result += "Giới Tính: " + eDocument.getOptionPerson().getSex() + "\n";//eDocument.getPersonDetails().getGender() + "\n";
        result += "Quốc Tịch: " + eDocument.getOptionPerson().getNationality() + "\n";
        result += "Dân Tộc " + eDocument.getOptionPerson().getNation() + "\n";
        result += "Tôn Giáo: " + eDocument.getOptionPerson().getReligion() + "\n";
        result += "Quê Quán: " + eDocument.getOptionPerson().getDistrict() + "\n";
        result += "Địa Chỉ Thường Trú: " + eDocument.getOptionPerson().getPermanentAddress() + "\n";
        result += "Đặc Điểm Nhận Diện: " + eDocument.getOptionPerson().getIdentifyingCharacteristics() + "\n";
        result += "Ngày Cấp: " + eDocument.getOptionPerson().getDateRange() + "\n";
        result += "Ngày Hết Hạn: " + eDocument.getOptionPerson().getDateExpiration() + "\n";
        result += "Họ Tên Bố: " + eDocument.getOptionPerson().getFatherName() + "\n";
        result += "Họ Tên Mẹ: " + eDocument.getOptionPerson().getMothername() + "\n";
        result += "Họ Tên Vợ/Chồng: " + eDocument.getOptionPerson().getHusbandAndWifeName() + "\n";
        result += "Số CMND: " + eDocument.getOptionPerson().getCMNDNumber() + "\n";
        result += "Passive Auth Successed: " + passiveAuthStr + "\n";
        result += "Chip Auth Succeeded: " + chipAuthStr + "\n";

        tvResult.setText(result);
    }

    private void clearViews() {
        ivPhoto.setImageBitmap(null);
        tvResult.setText("");
    }
}