package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data  //toString 이없으면 객체 출력시 해시값으로 나옴 toString이 있으면 값으로 찍혀나옴
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection //해당 어노테이션이 있을경우 컴파일 단계에서 오류를 확인할 수 있다.(다만 해당 DTO를 QClass로 만들어야 하기 때문에 compileQuerydsl을 해줘야함)
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }

    public MemberDto() {
    }
}
