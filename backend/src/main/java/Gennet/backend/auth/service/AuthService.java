package Gennet.backend.auth.service;

import Gennet.backend.auth.entity.RefreshToken;
import Gennet.backend.auth.filter.JwtAuthenticationFilter;
import Gennet.backend.auth.jwt.JwtTokenizer;
import Gennet.backend.auth.repository.AuthRepository;
import Gennet.backend.exception.BusinessLogicException;
import Gennet.backend.exception.ExceptionCode;
import Gennet.backend.member.entity.Member;
import Gennet.backend.member.repository.MemberRepository;
import Gennet.backend.member.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenizer jwtTokenizer;
    private final AuthRepository authRepository;
    private final MemberService memberService;
    private final MemberRepository memberRepository;

    /** Access Token 재발급 **/
    public void reissue(HttpServletRequest request, HttpServletResponse response){

        // TODO : 토큰 재발급
        //        만료된 토큰인지 체크?
        //        header로 받은 refreshToken과 db에 저장된 refreshToken이 같은지 검증 -> 같으면 발급, 다르면 exception
        //        member 객체를 구해야 jwtTokenizer에서 새로운 accessToken을 발급해줄 수 있음
        // TODO : 로그아웃
        //        refreshToken을 지우면 됨

        String refreshToken = request.getHeader("Refresh");

        Member member = findMemberByRefreshToken(refreshToken);

        RefreshToken storedRefreshToken = authRepository.findRefreshTokenByMemberId(member.getMemberId());


        if(refreshToken.equals(storedRefreshToken.getRefreshToken())){
            String accessToken = jwtTokenizer.delegateAccessToken(member);
            response.setHeader("Authorization", "Bearer " + accessToken);
        }
        else
            throw new BusinessLogicException(ExceptionCode.REFRESH_TOKEN_NOT_SAME);
        
    }
    /** 로그아웃 구현 **/
    public void logout(HttpServletRequest request) {

        String refreshToken = request.getHeader("Refresh");

        Member member = findMemberByRefreshToken(refreshToken);

        authRepository.deleteRefreshTokenByMemberId(member.getMemberId());
    }

    public Member findMemberByRefreshToken(String refreshToken){

        Jws<Claims> claims = jwtTokenizer.verifySignature(refreshToken);
        String email = claims.getBody().getSubject();

        Optional<Member> optionalMember = memberRepository.findByEmail(email);
        Member member = optionalMember.orElseThrow(() ->
                new BusinessLogicException(ExceptionCode.MEMBER_NOT_FOUND));

        return member;
    }
}
