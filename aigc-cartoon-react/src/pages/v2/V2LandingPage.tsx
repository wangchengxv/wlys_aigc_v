import V2Nav from './V2Nav';
import landingBg from '@/assets/figma/cartoon-workflow-v2/v2-landing-bg.png';
import '@/styles/v2-landing-page.css';

export default function V2LandingPage() {
  return (
    <div className="v2-landing" style={{ backgroundImage: `url(${landingBg})` }}>
      <div className="v2-landing__brand">Miioo</div>
      <div className="v2-landing__avatar" />
      <V2Nav />
      <button className="v2-landing__cta" type="button" onClick={() => window.location.href = '/v2/projects'}>
        开始创作
      </button>
    </div>
  );
}