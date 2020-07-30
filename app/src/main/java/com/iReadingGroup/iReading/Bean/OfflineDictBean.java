package com.iReadingGroup.iReading.Bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;

@Entity(nameInDb = "WordInfo", createInDb = false)
public class OfflineDictBean {
    @Property(nameInDb = "id")
    @Id(autoincrement = true)//key = id
    private long id;
    @Property(nameInDb = "word")
    private String word;
    @Property(nameInDb = "meaning")
    private String meaning;
    @Property(nameInDb = "sentence")
    private String sentence;

    @Generated(hash = 1101762644)
    public OfflineDictBean(long id, String word, String meaning, String sentence) {
        this.id = id;
        this.word = word;
        this.meaning = meaning;
        this.sentence = sentence;
    }


    @Generated(hash = 224238106)
    public OfflineDictBean() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getWord() {
        return this.word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getMeaning() {
        return this.meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public String getSentence() {
        return this.sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }


}