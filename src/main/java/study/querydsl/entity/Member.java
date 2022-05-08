package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  //기본생성자를 protected로 생성
@ToString(of = {"id", "username", "age"})   //toString을 생성해줌, team의 경우 무한루프에 빠질 수 있어 제외
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String username;

    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    public Member(String username) {
        this(username, 0);
    }

    public Member(String username, int age) {
        this(username, age, null);
    }

    //양방향 연관관계 매핑
    private void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}
