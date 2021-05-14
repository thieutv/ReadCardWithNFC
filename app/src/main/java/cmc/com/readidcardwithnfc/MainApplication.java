package cmc.com.readidcardwithnfc;

import androidx.multidex.MultiDexApplication;

//import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

public class MainApplication extends MultiDexApplication {
//    static {
//        Security.removeProvider("BC");
//        // Confirm that positioning this provider at the end works for your needs!
//        Security.addProvider(new BouncyCastleProvider());
//    }
    @Override
    public void onCreate() {
        super.onCreate();
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        //Security.removeProvider("BC");
        //Security.addProvider(new BouncyCastleProvider());
    }

}
