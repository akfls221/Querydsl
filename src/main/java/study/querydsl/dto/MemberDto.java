package study.querydsl.dto;

import lombok.Data;

@Data  //toString 이없으면 객체 출력시 해시값으로 나옴 toString이 있으면 값으로 찍혀나옴
public class MemberDto {

    private String username;
    private int age;

    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }

    public MemberDto() {
    }
}
