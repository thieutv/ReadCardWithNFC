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

        mainLayout = findViewById(R.id.main_layout);//ch???a ???nh ?????i di???n c???a t??i kho???n id card m???c ?????nh kh??ng ???????c hi???n th???
        loadingLayout = findViewById(R.id.loading_layout);//Ch???a loading ch??? load d??? li???u, m???c ?????nh kh??ng hi???n th???
        imageLayout = findViewById(R.id.image_layout);//Ch???a h??nh ???nh v??? find id card, m???c ?????nh gone
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
            Log.d("hau", "---???? ph??t hi???n th??? NFC---");
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

        //giao th???c x??c th???c chip Extended Access Control (EAC).

        /**
         * X??c th???c chip l?? m???t t??nh n??ng an to??n ki???m tra t??nh x??c th???c c???a chip, ch???ng l???i vi???c thay th??? nh??n b???n chip.
         * eMRTD s??? d???ng c?? ch??? sau ????? x??c th???c chip:
         * 1. X??c th???c ch??? ?????ng (Active authentication) n???u c?? d??? li???u v??? kh??a c??ng khai c???a ch??p eMRTD trong DG15. H??? th???ng ki???m tra v?? ?????c d??? li???u trong DG15
         * v?? th???c hi???n x??c th???c ch??? ?????ng.
         * 2. X??c th???c chip n???u c?? d??? li???u SecurityInfos trong DG14. H??? th???ng ki???m tra s??? ?????c DG14 v?? th???c hi???n x??c th???c chip.
         * 3. Th???c hi???n PACE v???i ??nh x??? x??c th???c chip (PACE-CAM) n???u c?? d??? li???u theo c???u tr??c PACEInfo trong file c?? b???n CardAccess. N???u PACE-CAM ???????c th???c hi???n
         * th??nh c??ng trong quy tr??nh truy c???p chip, thi???t b??? ?????u cu???i c?? th??? th???c hi???n c??c thao t??c sau ????? x??c th???c chip.
         * - X??c th???c ch??? ?????ng: l?? t??nh n??ng an to??n ng??n ch???n nh??n b???n chip b???ng c??ch b??? sung th??m c???p kh??a d??nh ri??ng cho chip:
         * kh??a c??ng khai ???????c l??u trong DG15 v?? ???????c b???o v??? b???i x??c th??c th??? ?????ng.
         * Kh??a ri??ng t????ng ???ng ???????c l??u trong v??ng nh??? an to??n v?? ch??? c?? th??? ???????c s??? d???ng b??n trong chip eMRTD v?? kh??ng th??? ?????c t??? b??n ngo??i.
         * Giao th???c Challenge-Respond ???????c chip eMRTD s??? d???ng ????? ch???ng minh tri th???c v??? kh??a ri??ng n??y. Trong giao th???c n??y chip eMRTD k?? s??? m???t th??ch
         * th???c (Gi?? tr??? ???????c ch???n ng???u nhi??n b???i thi???t b??? ?????u cu???i). Thi???t b??? ?????u cu???i ki???m tra ck do eMRTD g???i ?????n n???u h???p l??? th?? xem chip eMRTD l??
         * nguy??n g???c.
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

        //Giao th???c x??c th???c chip Passive Authentication (PA).

        /**
         * Th???c hi???n x??c th???c th??? ?????ng (B???t bu???c) nh???m kh???ng ?????nh d??? li???u trong chip eMRTD (c??c nh??m d??? li???u DG1-DG16 v?? SOD trong c???u tr??c d??? li???u LDS)
         * l?? x??c th???c (Bi???t ???????c ngu???n g???c c???a d??? li???u) v?? to??n ven (Ch??a b??? thay ?????i). v?? kh??ng y??u c???u kh??? n??ng x??? l?? c???a chip trong eMRTD, n??n ???????c
         * g???i l?? x??c th???c th??? ?????ng ?????i v???i n???i dung c???a chip eMRTD.
         * <p>
         * C??ch th???c hi???n: b1 - ?????c ?????i t?????ng an to??n t??i li???u SOD t??? chip eMRTD.
         * b2 - L???y ch???ng th?? s??? document signer, ch???ng th?? s??? CSCA v?? danh s??ch thu h???i ch???ng th?? s??? t??? PKD
         * b3 - Ki???m tra ch??? k?? s??? Ch???ng th?? s??? document signer, Ch???ng th?? s??? CSCA v?? ch??? k?? s?? c???a ?????i t?????ng an to??n t??i li???u (SOD)
         * b4 - T??nh gi?? tr??? b??m c???a c??c nh??m d??? li???u ???? ?????c v?? so s??nh ch??ng v???i c??c gi?? tr??? b??m trong ?????i t?????ng an to??n d??? li???u (SOD)
         * X??c th???c th??? ?????ng cho ph??p thi???t b??? ?????u cu???i ph??t hi???n c??c nh??m d??? li???u b??? s???a ?????i. n?? s??? d???ng cks k??m theo h??? t???ng m???t m?? kh??a c??ng khai.
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
                    // L???y CSCA t??? german master list
                    ASN1InputStream asn1InputStream = new ASN1InputStream(getAssets().open("CSCAVietnam.crt")); //CSCAVietnam.pem
                    //ASN1InputStream asn1InputStream = new ASN1InputStream(getAssets().open("CSCAVietnam.pem"));
                    ASN1Primitive p;
                    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                    keystore.load(null, null);
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    int a = 0;
                    while ((p = asn1InputStream.readObject()) != null) {
                        a++;
                        Log.d("hau2", "gi?? tr??? v??ng l???p " + a);
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
                    //Ch??ng t??i ki???m tra xem ch???ng ch??? c?? ???????c k?? b???i CSCA ????ng tin c???y hay kh??ng
                    // TODO: verify if certificate is revoked (x??c m??nh ch???ng ch??? c?? b??? thu h???i hay kh??ng).
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
                //B?????c 1 ?????c EF.CardAccess
                CardService cardService = CardService.getInstance(isoDep);
                cardService.open();

                // B?????c 2,3 4 th???c hi???n giao th???c ki???m so??t truy nh???p PACE (n???u ???????c h??? tr???).
                //Ch???n ???ng d???ng ePassport
                //Th???c hi???n giao th???c ki???m so??t truy nh???p BAC n???u nh?? PACE kh??ng ???????c h??? tr???.

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

                //Th???c hi???n x??c th???c ch??? ?????ng (X??c th???c chip) t??y ch???n c?? th??? th???c hi???n ho???c kh??ng.
                //Th???c hi???n x??c th???c chip (perform Chip Authentication) b???ng nh??m d??? li???u DG14
                doChipAuth(service);

                //sau ???? x??c th???c th??? (Passive Authentication) b???ng SODFile
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

                //?????c d??? li???u DG15
                Log.d("hau1", "==== DG15 Public key: " + dg15File.getPublicKey());
                Log.d("hau1", "==== DG15 Algorithm: " + dg15File.getPublicKey().getAlgorithm());
                Log.d("hau1", "==== DG15 public key format: " + dg15File.getPublicKey().getFormat());
                //?????c d??? li???u dg 12

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
                Log.d("hau", "L???i ngo??i c??ng: " + e.getMessage());
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

        String result = "H??? V?? T??n: " + eDocument.getOptionPerson().getName() + "\n";//eDocument.getPersonDetails().getName() + "\n";
        //result += "SURNAME: " + eDocument.getPersonDetails().getSurname() + "\n";
        //result += "PERSONAL NUMBER: " + eDocument.getPersonDetails().getPersonalNumber() + "\n";
        result += "S??? C??n C?????c C??ng D??n: " + eDocument.getOptionPerson().getCCCDNumber() + "\n";
        result += "Ng??y Sinh: " + eDocument.getOptionPerson().getDateOfBirth() + "\n";
        result += "Gi???i T??nh: " + eDocument.getOptionPerson().getSex() + "\n";//eDocument.getPersonDetails().getGender() + "\n";
        result += "Qu???c T???ch: " + eDocument.getOptionPerson().getNationality() + "\n";
        result += "D??n T???c " + eDocument.getOptionPerson().getNation() + "\n";
        result += "T??n Gi??o: " + eDocument.getOptionPerson().getReligion() + "\n";
        result += "Qu?? Qu??n: " + eDocument.getOptionPerson().getDistrict() + "\n";
        result += "?????a Ch??? Th?????ng Tr??: " + eDocument.getOptionPerson().getPermanentAddress() + "\n";
        result += "?????c ??i???m Nh???n Di???n: " + eDocument.getOptionPerson().getIdentifyingCharacteristics() + "\n";
        result += "Ng??y C???p: " + eDocument.getOptionPerson().getDateRange() + "\n";
        result += "Ng??y H???t H???n: " + eDocument.getOptionPerson().getDateExpiration() + "\n";
        result += "H??? T??n B???: " + eDocument.getOptionPerson().getFatherName() + "\n";
        result += "H??? T??n M???: " + eDocument.getOptionPerson().getMothername() + "\n";
        result += "H??? T??n V???/Ch???ng: " + eDocument.getOptionPerson().getHusbandAndWifeName() + "\n";
        result += "S??? CMND: " + eDocument.getOptionPerson().getCMNDNumber() + "\n";
        result += "Passive Auth Successed: " + passiveAuthStr + "\n";
        result += "Chip Auth Succeeded: " + chipAuthStr + "\n";

        tvResult.setText(result);
    }

    private void clearViews() {
        ivPhoto.setImageBitmap(null);
        tvResult.setText("");
    }
}