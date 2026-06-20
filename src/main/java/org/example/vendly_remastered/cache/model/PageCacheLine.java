package org.example.vendly_remastered.cache.model;

import javafx.scene.Parent;
import org.example.vendly_remastered.constants.Constants;

public class PageCacheLine {

    private int ALN; //Access logical number, a timestamp for the last usage of the page
    private Parent page;
    private int priority;
    private Constants pageName;

    public PageCacheLine(int aln, Parent targetPage, int priority, Constants pageName){
        this.page = targetPage;
        this.priority = priority;
        this.ALN = aln;
        this.pageName = pageName;
    }
    public int getPriority(){
        return this.priority;
    }
    public int getALN(){
        return this.ALN;
    }
    public Parent getPage(){
        return this.page;
    }
    public void setALN(int aln){
        this.ALN = aln;
    }
    public Constants getPageName(){return pageName; }
}
