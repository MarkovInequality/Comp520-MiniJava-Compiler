/**
 * COMP 520
 * check short circuit conditional evaluation
 */
class MainClass {
    public static void main (String [] args) {
        
        MainClass m = new MainClass ();
        m.didrun = false;
        int res = 23;
        boolean t = true;
        boolean f = false;
        
        if ( (t || m.dontrun(t)) && f && m.dontrun(t))
            res = -1;
        if ( ! ((t && t && f && m.dontrun(t)) || f || f || t || m.dontrun(t)) )
            res = -1;
        if (m.didrun)
            res = -1;
        System.out.println(res);
    }

    public boolean didrun;

    public boolean dontrun(boolean r) {
        didrun = true;
        return r;
    }
}

