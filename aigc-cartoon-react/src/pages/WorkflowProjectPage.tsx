import { useState } from 'react';
import { DownOutlined, PlusOutlined } from '@ant-design/icons';
import '@/styles/workflow-project-page.css';

interface Episode {
  id: number;
  title: string;
  isActive?: boolean;
}

const episodes: Episode[] = [
  { id: 1, title: '第一集：陌生的邻居', isActive: true },
  { id: 2, title: '第二集：意外的援手' },
  { id: 3, title: '第三集：寻食大挑战' },
  { id: 4, title: '第四集：山谷的秘密' },
  { id: 5, title: '第五集：并肩同行' },
];

const stepItems = [
  { id: 1, label: '全局设定' },
  { id: 2, label: '剧本' },
  { id: 3, label: '主体' },
  { id: 4, label: '分镜' },
  { id: 5, label: '剪辑成片' },
];

function WorkflowProjectPage() {
  const [activeStep, setActiveStep] = useState(2);
  const [selectedEpisode, setSelectedEpisode] = useState(episodes[0]);

  return (
    <div className="workflow-project-page">
      <header className="workflow-project-page__header">
        <div className="workflow-project-page__logo">logo</div>
        <div className="workflow-project-page__avatar" />
      </header>

      <div className="workflow-project-page__layout">
        <aside className="workflow-project-page__steps">
          {stepItems.map((item) => (
            <div
              key={item.id}
              className={`workflow-project-page__step-item ${
                activeStep === item.id ? 'workflow-project-page__step-item--active' : ''
              }`}
              onClick={() => setActiveStep(item.id)}
            >
              <div className="workflow-project-page__step-bar" />
              <span>{item.label}</span>
            </div>
          ))}
        </aside>

        <main className="workflow-project-page__content">
          <div className="workflow-project-page__script-section">
            <div className="workflow-project-page__script-header">
              <div className="workflow-project-page__script-tabs">
                {episodes.map((episode) => (
                  <div
                    key={episode.id}
                    className={`workflow-project-page__script-tab ${
                      selectedEpisode.id === episode.id
                        ? 'workflow-project-page__script-tab--active'
                        : ''
                    }`}
                    onClick={() => setSelectedEpisode(episode)}
                  >
                    {episode.title}
                  </div>
                ))}
              </div>
              <p className="workflow-project-page__script-hint">
                剧本已完成 5 集分集，涵盖相遇、互助、冒险等剧情，贴合漫剧呈现需求。需要我帮你调整某一集的节奏，让剧情更紧凑吗？
              </p>
            </div>
            <div className="workflow-project-page__script-content">
              <h3>两只老虎的青枫奇遇</h3>
              <p>
                第一集：陌生的邻居
                <br />
                <br />
                场景1：青枫林·晨光草地（0:00-1:30）
                <br />
                【镜头1】俯拍：青枫林被晨雾笼罩，金黄的枫叶落在草地上，露珠折射阳光。背景音乐：轻快的森林鸟鸣声，轻柔的钢琴旋律。
                <br />
                【镜头2】近景：橙色皮毛的小老虎乐乐，额头上的"王"字歪歪扭扭，尾巴翘得老高，正追着一只蝴蝶跑，爪子偶尔扒拉一下地上的枫叶。
                <br />
                乐乐（活泼大喊，耳朵抖动）：别跑呀！陪我玩一会儿～
                <br />
                【镜头3】特写：蝴蝶停在一片枫叶上，乐乐猛地扑过去，摔了个四脚朝天，肚皮露在外面，一脸懊恼。
                <br />
                乐乐（揉着鼻子，小声嘟囔）：哼，居然敢耍我！
                <br />
                <br />
                场景2：青枫林·枫树下（1:30-4:00）
                <br />
                【镜头1】侧拍：一棵粗壮的枫树下，白色皮毛的小老虎安安正安静地趴在石头上，闭着眼睛晒太阳，耳朵时不时动一下，警惕周围的动静。它的"王"字整齐端正，眼神沉稳。
                <br />
                【镜头2】全景：乐乐摔疼后，抬头看到安安，眼睛一亮，尾巴甩得更欢，踮着脚尖跑过去。
                <br />
                乐乐（凑到安安身边，小声试探）：喂！你是谁呀？怎么和我长得不一样？你也是老虎吗？
                <br />
                【镜头3】特写：安安缓缓睁开眼睛，眼神冷淡，瞥了乐乐一眼，没有说话，只是往石头里面挪了挪，避开乐乐。
                <br />
                乐乐（不气馁，凑得更近，鼻子快碰到安安的皮毛）：我叫乐乐！我住在这片林子的东边，你住在这里吗？我们一起玩好不好？
                <br />
                【镜头4】中景：安安猛地站起身，耳朵竖起来，对着乐乐低吼了一声，转身跳进枫树林深处，只留下一个白色的背影。
                <br />
                乐乐（被吓了一跳，往后退了两步，挠了挠头）：奇怪，它怎么不理我呀？
                <br />
                <br />
                场景3：青枫林·小溪边（4:00-6:00）
                <br />
                【镜头1】中景：乐乐跟着安安的脚印，走到小溪边，看到安安正低头喝水，动作优雅，尾巴轻轻搭在地上。
                <br />
                【镜头2】近景：乐乐小心翼翼地走过去，蹲在小溪另一边，也低下头喝水，时不时偷偷瞥向安安。
                <br />
                乐乐（小声说）：我知道啦，你是不是不喜欢热闹？我不吵你，我们一起喝水好不好？
                <br />
                【镜头3】特写：安安喝水的动作顿了一下，没有回头，也没有回应，但尾巴轻轻动了一下，没有再赶走乐乐。
                <br />
                【镜头4】全景：阳光穿透枫叶，洒在两只老虎身上，小溪潺潺流淌，画面安静又温暖。
                <br />
                【音效】轻微的水流声，远处的鸟鸣声，背景音乐渐弱。
                <br />
                【结尾字幕】下集预告：意外降临，乐乐陷入麻烦，安安会出手相助吗？
                <br />
                <br />
                第二集：意外的援手
                <br />
                <br />
                场景1：青枫林·灌木丛（0:00-2:00）
                <br />
                【镜头1】中景：乐乐蹦蹦跳跳地在灌木丛中穿梭，时不时用爪子扒拉一下灌木丛，寻找野果。背景音乐：活泼的鼓点，搭配鸟鸣声。
                <br />
                乐乐（一边找一边哼歌）：找呀找呀找野果，找到一颗大野果～
                <br />
                【镜头2】特写：乐乐看到一颗红彤彤的野果，挂在低矮的灌木丛上，眼睛一亮，猛地扑过去。
                <br />
                【镜头3】中景：乐乐扑空，不小心踩空了脚下的土坡，滚进了一个浅浅的土坑，被藤蔓缠住了后腿，动弹不得。
                <br />
                乐乐（挣扎了几下，语气慌乱）：哎呀！谁来救我呀？我的腿被缠住了！
                <br />
                <br />
                场景2：青枫林·土坑旁（2:00-5:00）
                <br />
                【镜头1】全景：安安慢悠悠地从枫树林里走出来，听到乐乐的呼救声，停下脚步，抬头望向土坑的方向，眼神犹豫了一下。
                <br />
                【镜头2】近景：安安迈开脚步，走到土坑边，低头看着坑里挣扎的乐乐，耳朵微微耷拉着。
                <br />
                乐乐（看到安安，眼睛瞬间亮了，语气带着委屈）：安安！快救我！我的腿动不了了！
                <br />
                【镜头3】特写：安安蹲下身，用爪子小心翼翼地拨开缠绕在乐乐后腿上的藤蔓，动作轻柔，避免弄疼乐乐。
                <br />
                乐乐（小声说）：谢谢你，安安。我不该这么调皮的。
                <br />
                【镜头4】中景：藤蔓被拨开，乐乐试着动了动后腿，虽然有点疼，但能站起来了。它慢慢爬到土坑边，安安伸出爪子，拉了它一把。
                <br />
                <br />
                场景3：青枫林·枫树下（5:00-7:00）
                <br />
                【镜头1】中景：安安带着乐乐回到之前的枫树下，让乐乐趴在石头上，用舌头轻轻舔了舔乐乐后腿上的伤口。
                <br />
                乐乐（舒服地眯起眼睛，小声说）：安安，你真好。以前都没有人这么关心我。
                <br />
                【镜头2】特写：安安停下动作，看了乐乐一眼，终于开口说话，声音低沉温柔。
                <br />
                安安：以后别乱跑，这里有很多危险。
                <br />
                【镜头3】全景：乐乐点点头，往安安身边凑了凑，两只老虎一起趴在石头上晒太阳，枫叶落在它们的身上，画面温馨。
                <br />
                【音效】轻柔的背景音乐，风吹枫叶的沙沙声。
                <br />
                【结尾字幕】下集预告：森林里的食物变少了，两只老虎决定一起寻找食物，它们会遇到什么困难？
              </p>
            </div>
          </div>

          <div className="workflow-project-page__settings">
            <div className="workflow-project-page__dropdown">
              <span>集数：自动适应</span>
              <DownOutlined />
            </div>

            <h2 className="workflow-project-page__title">两只老虎的故事</h2>

            <button type="button" className="workflow-project-page__add-btn">
              <PlusOutlined />
            </button>

            <div className="workflow-project-page__model-dropdown">
              <span>Doubao-Seed-2.0-Pro</span>
              <DownOutlined />
            </div>

            <div className="workflow-project-page__avatar-large" />
          </div>
        </main>
      </div>
    </div>
  );
}

export default WorkflowProjectPage;