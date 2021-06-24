package cmc.com.readidcardwithnfc.jmrtd;

import android.os.Parcel;
import android.os.Parcelable;

public class FeatureStatus implements Parcelable {
    private Verdict hasSAC;
    private Verdict hasBAC;
    private Verdict hasAA;
    private Verdict hasEAC;
    private Verdict hasCA;
    private Verdict hasPACE;

    protected FeatureStatus(Parcel in) {
        if (in.readInt() == 1)
            this.hasSAC = Verdict.valueOf(in.readString());
        else this.hasSAC = null;
        if (in.readInt() == 1)
            this.hasBAC = Verdict.valueOf(in.readString());
        else this.hasBAC = null;
        if (in.readInt() == 1)
            this.hasAA = Verdict.valueOf(in.readString());
        else this.hasAA = null;
        if (in.readInt() == 1)
            this.hasEAC = Verdict.valueOf(in.readString());
        else this.hasEAC = null;
        if (in.readInt() == 1)
            this.hasCA = Verdict.valueOf(in.readString());
        else this.hasCA = null;
        if (in.readInt() == 1)
            this.hasPACE = Verdict.valueOf(in.readString());
        else this.hasPACE = null;

    }

    public static final Creator<FeatureStatus> CREATOR = new Creator<FeatureStatus>() {
        @Override
        public FeatureStatus createFromParcel(Parcel in) {
            return new FeatureStatus(in);
        }

        @Override
        public FeatureStatus[] newArray(int size) {
            return new FeatureStatus[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (this.hasSAC != null) {
            dest.writeInt(1);
            dest.writeString(this.hasSAC.name());
        } else {
            dest.writeInt(0);
        }
        if (this.hasBAC != null) {
            dest.writeInt(1);
            dest.writeString(this.hasBAC.name());
        } else {
            dest.writeInt(0);
        }
        if (this.hasAA != null) {
            dest.writeInt(1);
            dest.writeString(this.hasAA.name());
        } else {
            dest.writeInt(0);
        }
        if (this.hasEAC != null) {
            dest.writeInt(1);
            dest.writeString(this.hasEAC.name());
        } else {
            dest.writeInt(0);
        }
        if (this.hasCA != null) {
            dest.writeInt(1);
            dest.writeString(this.hasCA.name());
        } else {
            dest.writeInt(0);
        }
        if (this.hasPACE != null) {
            dest.writeInt(1);
            dest.writeString(this.hasPACE.name());
        } else {
            dest.writeInt(0);
        }


    }

    enum Verdict {
        UNKNOWN, /* Presence unknown */
        PRESENT, /* Present */
        NOT_PRESENT
        /* Not present */
    }

    public FeatureStatus(Verdict hasSAC, Verdict hasBAC, Verdict hasAA, Verdict hasEAC, Verdict hasCA, Verdict hasPACE) {
        this.hasSAC = Verdict.UNKNOWN;
        this.hasBAC = Verdict.UNKNOWN;
        this.hasAA = Verdict.UNKNOWN;
        this.hasEAC = Verdict.UNKNOWN;
        this.hasCA = Verdict.UNKNOWN;
        this.hasPACE = Verdict.UNKNOWN;
    }

    public Verdict getHasSAC() {
        return hasSAC;
    }

    public void setHasSAC(Verdict hasSAC) {
        this.hasSAC = hasSAC;
    }

    public Verdict getHasBAC() {
        return hasBAC;
    }

    public void setHasBAC(Verdict hasBAC) {
        this.hasBAC = hasBAC;
    }

    public Verdict getHasAA() {
        return hasAA;
    }

    public void setHasAA(Verdict hasAA) {
        this.hasAA = hasAA;
    }

    public Verdict getHasEAC() {
        return hasEAC;
    }

    public void setHasEAC(Verdict hasEAC) {
        this.hasEAC = hasEAC;
    }

    public Verdict getHasCA() {
        return hasCA;
    }

    public void setHasCA(Verdict hasCA) {
        this.hasCA = hasCA;
    }

    public Verdict getHasPACE() {
        return hasPACE;
    }

    public void setHasPACE(Verdict hasPACE) {
        this.hasPACE = hasPACE;
    }
}
