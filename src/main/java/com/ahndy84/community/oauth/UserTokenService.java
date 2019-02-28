package com.ahndy84.community.oauth;

import com.ahndy84.community.domain.enums.SocialType;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.List;
import java.util.Map;

/**
 * User 정보를 비동기 통신으로 가져오는 REST Service인 UserinfoTokenServices를 커스터마이징할 UserTokenService를 생성.
 * 소셜 미디어 원격서버와 통신하여 User정보를 가져오는 로직은 이미 UserInfoTokenServices에 구현되어 있어 UserTokenService에서는 이를 상속받아 통신에 필요한 값을 넣어주어 실행
 */
public class UserTokenService extends UserInfoTokenServices {

	public UserTokenService(ClientResources resources, SocialType socialType) {
		super(resources.getResource().getUserInfoUri(), resources.getClient().getClientId()); //super를 사용하여 각각의 소셜 미디어 정보를 주입할 수 있도록 합니다.
		setAuthoritiesExtractor(new Oauth2AuthoritiesExtractor(socialType));
	}

	/**
	 * Oauth2AuthoritiesExtractor는 UserTokenService의 부모클래스인 UserinfotokenServices의 setAuthoritiesExtractor메소드를 이용하여 등록
	 */
	public static class Oauth2AuthoritiesExtractor implements AuthoritiesExtractor {
		private String socialType;

		public Oauth2AuthoritiesExtractor(SocialType socialType) {
			this.socialType = socialType.getRoleType();
		}

		@Override
		public List<GrantedAuthority> extractAuthorities(Map<String, Object> map) {
			return AuthorityUtils.createAuthorityList(this.socialType);
		}
	}
}
